package fr.kfokam48.exercice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.referentiel.Promotion;
import fr.kfokam48.referentiel.PromotionRepository;
import fr.kfokam48.session.Session;
import fr.kfokam48.session.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/exercices} (EF5, RG15, RG19, RG22).
 *
 * <p>Le test central est celui de Q12 : un depot doit etre accepte <b>apres</b>
 * l'expiration du code de presence, tant que la session n'est pas close.</p>
 *
 * <p>Chaque test ouvre sa propre session : l'unicite (session, etudiant) ne peut donc
 * pas creer d'interference entre tests. On utilise l'etudiant 2 de la promotion 1, et
 * l'etudiant 13 de la promotion 2 pour le controle RG19.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExerciceControllerIT {

    private static final long PROMOTION_1 = 1L;
    private static final long ETUDIANT_PROMOTION_1 = 2L;
    private static final long ETUDIANT_PROMOTION_2 = 13L;
    private static final String LIEN_VALIDE = "https://example.invalid/rendus/2-exercice4.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    // ------------------------------------------------------------------ nominal

    @Test
    @DisplayName("un depot valide renvoie 201 avec un statut d'attente (EF5)")
    void depose_un_exercice_valide() throws Exception {
        JsonNode session = ouvrirSessionParApi("Seance depot");
        long sessionId = session.get("id").asLong();

        String corps = deposer(sessionId, ETUDIANT_PROMOTION_1, LIEN_VALIDE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(corps);
        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "statut");
        assertThat(exerciceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("un depot reste possible apres l'expiration du code (Q12, RG15)")
    void accepte_un_depot_apres_l_expiration_du_code() throws Exception {
        // Code expire depuis 20 minutes, session non close : c'est exactement le cas de
        // l'etudiant sans connexion le soir meme (Q12).
        Session session = enregistrerSession("EEEEE2", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20), false);

        deposer(session.getId(), ETUDIANT_PROMOTION_1, LIEN_VALIDE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    // ------------------------------------------------------------------ erreurs

    @Test
    @DisplayName("un second depot pour la meme session renvoie 409 sans seconde ligne (EF5)")
    void refuse_un_second_depot_pour_la_meme_session() throws Exception {
        long sessionId = ouvrirSessionParApi("Seance doublon depot").get("id").asLong();

        deposer(sessionId, ETUDIANT_PROMOTION_1, LIEN_VALIDE).andExpect(status().isCreated());

        deposer(sessionId, ETUDIANT_PROMOTION_1, "https://example.invalid/rendus/2-v2.pdf")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(exerciceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("un lien invalide renvoie 400 LIEN_INVALIDE (EF5, RG22)")
    void refuse_un_lien_invalide() throws Exception {
        long sessionId = ouvrirSessionParApi("Seance lien invalide").get("id").asLong();

        // absent
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d }
                                """.formatted(sessionId, ETUDIANT_PROMOTION_1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));

        // relatif
        deposer(sessionId, ETUDIANT_PROMOTION_1, "/rendus/2.pdf")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));

        // scheme detourne
        deposer(sessionId, ETUDIANT_PROMOTION_1, "javascript:alert(1)")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(exerciceRepository.countBySession_Id(sessionId)).isZero();
    }

    @Test
    @DisplayName("une session close refuse le depot : 409 SESSION_CLOTUREE (Q12, RG21)")
    void refuse_un_depot_sur_session_close() throws Exception {
        Session session = enregistrerSession("EEEEE3", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), true);

        deposer(session.getId(), ETUDIANT_PROMOTION_1, LIEN_VALIDE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un etudiant d'une autre promotion est refuse : 403 ACCES_REFUSE (RG19)")
    void refuse_un_etudiant_d_une_autre_promotion() throws Exception {
        long sessionId = ouvrirSessionParApi("Seance promotion 1").get("id").asLong();

        deposer(sessionId, ETUDIANT_PROMOTION_2, LIEN_VALIDE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("une session inconnue renvoie 404 SESSION_INTROUVABLE (EF5)")
    void refuse_une_session_inconnue() throws Exception {
        deposer(999999L, ETUDIANT_PROMOTION_1, LIEN_VALIDE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INTROUVABLE"));
    }

    @Test
    @DisplayName("un etudiant inconnu renvoie 404 ETUDIANT_INCONNU (EF5)")
    void refuse_un_etudiant_inconnu() throws Exception {
        long sessionId = ouvrirSessionParApi("Seance etudiant inconnu").get("id").asLong();

        deposer(sessionId, 999999L, LIEN_VALIDE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    // ------------------------------------------------------------------ outils

    private ResultActions deposer(long sessionId, long etudiantId, String lien) throws Exception {
        return mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                        """.formatted(sessionId, etudiantId, lien)));
    }

    private JsonNode ouvrirSessionParApi(String titre) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "%s", "promotionId": %d }
                                """.formatted(titre, PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps);
    }

    /** Session fabriquee directement en base, pour maitriser ouverture et cloture. */
    private Session enregistrerSession(String code, LocalDateTime ouvertureAt, boolean close) {
        Promotion promotion = promotionRepository.findById(PROMOTION_1).orElseThrow();
        Session session = new Session(promotion, "Session de test", code, ouvertureAt);
        if (close) {
            session.cloturer(ouvertureAt.plusMinutes(1));
        }
        return sessionRepository.save(session);
    }
}
