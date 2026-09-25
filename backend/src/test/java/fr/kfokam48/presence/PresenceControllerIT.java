package fr.kfokam48.presence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.referentiel.EtudiantRepository;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/presences} (EF2, EF3, RG2, RG3, RG4, RG19).
 *
 * <p>Ils reprennent les trois scenarios du diagramme {@code docs/diagrammes/D3.md}
 * (nominal, {@code 410 CODE_EXPIRE}, {@code 409 DEJA_PRESENT}) et couvrent les cas
 * limites que le diagramme annonce : priorite de l'expiration sur l'unicite, RG19,
 * session close et code inconnu.</p>
 *
 * <p>Donnees de reference (migration V2) : promotion 1 = etudiants 1 a 12,
 * promotion 2 = etudiants 13 a 16.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceControllerIT {

    private static final long PROMOTION_1 = 1L;
    private static final long PREMIER_ETUDIANT_PROMOTION_1 = 1L;
    private static final long PREMIER_ETUDIANT_PROMOTION_2 = 13L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    // ------------------------------------------------------------------ nominal

    @Test
    @DisplayName("un code valide marque la presence avec source ETUDIANT (EF2, D3 scenario 1)")
    void marque_la_presence_avec_un_code_valide() throws Exception {
        JsonNode session = ouvrirSessionParApi("Seance nominale");
        long sessionId = session.get("id").asLong();
        String code = session.get("code").asText();

        String corps = mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(code, PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.etudiantId").value(PREMIER_ETUDIANT_PROMOTION_1))
                .andExpect(jsonPath("$.source").value("ETUDIANT"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(corps);
        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "sessionId", "etudiantId", "source");
        assertThat(presenceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("le code est normalise : casse et espaces n'empechent pas la saisie (H9)")
    void accepte_un_code_saisi_en_minuscules_avec_des_espaces() throws Exception {
        String code = ouvrirSessionParApi("Seance casse").get("code").asText();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "  %s  ", "etudiantId": %d }
                                """.formatted(code.toLowerCase(java.util.Locale.ROOT), PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    // ------------------------------------------------------------------ erreurs

    @Test
    @DisplayName("un second vote pour la meme session renvoie 409 DEJA_PRESENT sans seconde ligne (RG4, D3 scenario 3)")
    void refuse_une_seconde_presence_pour_la_meme_session() throws Exception {
        JsonNode session = ouvrirSessionParApi("Seance doublon");
        long sessionId = session.get("id").asLong();
        String code = session.get("code").asText();

        marquerPresence(code, PREMIER_ETUDIANT_PROMOTION_1).andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(code, PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        // La contrainte d'unicite n'a pas ete contournee : toujours une seule ligne.
        assertThat(presenceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("un code expire renvoie 410 CODE_EXPIRE, pas 409 (RG2, D3 scenario 2)")
    void refuse_un_code_expire_avec_410() throws Exception {
        Session session = enregistrerSession("ZZZZZ2", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20), false);

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(session.getCode(), PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(presenceRepository.countBySession_Id(session.getId())).isZero();
    }

    @Test
    @DisplayName("l'expiration prime sur DEJA_PRESENT : un etudiant deja present sur un code expire obtient 410")
    void l_expiration_prime_sur_le_doublon() throws Exception {
        Session session = enregistrerSession("YYYYY3", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30), false);
        Etudiant etudiant = etudiantRepository.findById(PREMIER_ETUDIANT_PROMOTION_1).orElseThrow();
        presenceRepository.saveAndFlush(
                new Presence(session, etudiant, SourcePresence.ETUDIANT, LocalDateTime.now(ZoneOffset.UTC)));

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(session.getCode(), PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    @DisplayName("un code inconnu renvoie 400 CODE_INCONNU (EF3)")
    void refuse_un_code_inconnu() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "QQQQQ2", "etudiantId": %d }
                                """.formatted(PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un code absent renvoie 400 CHAMP_MANQUANT (ENF4)")
    void refuse_un_code_absent() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "etudiantId": %d }
                                """.formatted(PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("un etudiant inconnu renvoie 404 ETUDIANT_INCONNU")
    void refuse_un_etudiant_inconnu() throws Exception {
        String code = ouvrirSessionParApi("Seance etudiant inconnu").get("code").asText();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": 999999 }
                                """.formatted(code)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un etudiant d'une autre promotion est refuse : 403 ACCES_REFUSE (RG19)")
    void refuse_un_etudiant_d_une_autre_promotion() throws Exception {
        String code = ouvrirSessionParApi("Seance promotion 1").get("code").asText();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(code, PREMIER_ETUDIANT_PROMOTION_2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("une session close refuse la presence : 409 SESSION_CLOTUREE (RG21)")
    void refuse_une_session_cloturee() throws Exception {
        Session session = enregistrerSession("XXXXX4", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), true);

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(session.getCode(), PREMIER_ETUDIANT_PROMOTION_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ------------------------------------------------------------------ outils

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

    private org.springframework.test.web.servlet.ResultActions marquerPresence(String code, long etudiantId)
            throws Exception {
        return mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "%s", "etudiantId": %d }
                        """.formatted(code, etudiantId)));
    }
}
