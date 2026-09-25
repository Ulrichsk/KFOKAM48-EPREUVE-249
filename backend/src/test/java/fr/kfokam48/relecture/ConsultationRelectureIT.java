package fr.kfokam48.relecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de la consultation des relectures (EF9, RG16, RG20) :
 * {@code GET /api/relectures} et {@code GET /api/relectures/{id}} (issue 09).
 *
 * <p>Etudiants utilises : 2 (auteur), 8 (relecteur) et 9 (intrus) de la promotion 1 —
 * les autres identifiants sont deja consommes par les autres classes de test, le
 * contexte Spring et la base H2 etant partages. Les relectures sont creees par la voie
 * nominale (session ouverte, presence du relecteur, depot de l'auteur, tirage), sauf
 * le cas « relecture inconnue » qui n'a besoin de rien.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConsultationRelectureIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 2L;
    private static final long RELECTEUR = 8L;
    private static final long INTRUS = 9L;
    private static final String LIEN = "https://example.invalid/rendus/2-exercice9.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // ------------------------------------------------------------- liste des relectures

    @Test
    @DisplayName("le relecteur voit ses relectures, sans aucune identite d'auteur (Q8, RG20)")
    void liste_les_relectures_du_relecteur_sans_identite_auteur() throws Exception {
        creerRelectureParTirage();

        String corps = mockMvc.perform(get("/api/relectures")
                        .header("X-Etudiant-Id", RELECTEUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode relectures = objectMapper.readTree(corps);
        assertThat(relectures).isNotEmpty();
        for (JsonNode relecture : relectures) {
            // Champs du contrat, et strictement rien d'autre.
            assertThat(relecture.fieldNames()).toIterable()
                    .containsExactlyInAnyOrder("id", "exerciceId", "statut", "assigneAt");
            // Anonymat : aucune cle ne fait reference a l'auteur ni au relecteur.
            assertThat(relecture.has("auteur")).isFalse();
            assertThat(relecture.has("auteurId")).isFalse();
            assertThat(relecture.has("relecteurId")).isFalse();
            assertThat(relecture.has("etudiantId")).isFalse();
            assertThat(relecture.get("statut").asText()).isEqualTo("EN_ATTENTE");
        }
    }

    @Test
    @DisplayName("un etudiant inconnu repond 404 ETUDIANT_INCONNU (contrat)")
    void refuse_un_etudiant_inconnu_sur_la_liste() throws Exception {
        mockMvc.perform(get("/api/relectures")
                        .header("X-Etudiant-Id", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ------------------------------------------------------------- detail du lien a relire

    @Test
    @DisplayName("la premiere consultation horodate le lien : 200 avec lienConsulteAt renseigne (Q13)")
    void consulte_le_lien_et_horodate_la_premiere_consultation() throws Exception {
        long relectureId = creerRelectureParTirage();

        Relecture avant = relectureRepository.findById(relectureId).orElseThrow();
        assertThat(avant.getLienConsulteAt()).isNull();

        String corps = mockMvc.perform(get("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", RELECTEUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relectureId))
                .andExpect(jsonPath("$.exerciceId").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.lien").value(LIEN))
                .andExpect(jsonPath("$.titreSession").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detail = objectMapper.readTree(corps);
        // Champs du contrat, et strictement rien d'autre : aucun nom d'auteur (Q8).
        assertThat(detail.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "exerciceId", "statut", "lien",
                        "titreSession", "lienConsulteAt");

        LocalDateTime horodatage = relectureRepository.findById(relectureId).orElseThrow()
                .getLienConsulteAt();
        assertThat(horodatage).isNotNull();

        // Une seconde consultation ne reecrit pas l'horodatage (premiere fois fait foi).
        Thread.sleep(5);
        mockMvc.perform(get("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", RELECTEUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienConsulteAt").isNotEmpty());

        LocalDateTime apres = relectureRepository.findById(relectureId).orElseThrow()
                .getLienConsulteAt();
        assertThat(apres).isEqualTo(horodatage);
    }

    @Test
    @DisplayName("un autre etudiant que le relecteur designe est refuse : 403 ACCES_REFUSE")
    void refuse_un_etudiant_qui_n_est_pas_le_relecteur() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(get("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", INTRUS))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Aucune consultation parasite : le lien n'a pas ete horodate.
        assertThat(relectureRepository.findById(relectureId).orElseThrow().getLienConsulteAt())
                .isNull();
    }

    @Test
    @DisplayName("l'auteur lui-meme ne peut pas ouvrir le lien de son exercice : 403 (Q8)")
    void refuse_l_auteur_de_l_exercice() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(get("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", AUTEUR))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    @DisplayName("une relecture inconnue repond 404 RELECTURE_INTROUVABLE (contrat)")
    void refuse_une_relecture_inconnue() throws Exception {
        mockMvc.perform(get("/api/relectures/{id}", 999_999L)
                        .header("X-Etudiant-Id", RELECTEUR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INTROUVABLE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("le header X-Etudiant-Id est obligatoire : 400 CHAMP_MANQUANT (contrat)")
    void exige_le_header_d_identite() throws Exception {
        mockMvc.perform(get("/api/relectures/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    // ------------------------------------------------------------- outils

    /**
     * Cree une relecture par la voie nominale : session ouverte, presence du relecteur,
     * depot de l'auteur, tirage au sort. Renvoie l'identifiant de la relecture.
     */
    private long creerRelectureParTirage() throws Exception {
        long sessionId = ouvrirSession();

        marquerPresence(sessionId, RELECTEUR).andExpect(status().isCreated());

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, AUTEUR, LIEN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long exerciceId = objectMapper.readTree(corps).get("id").asLong();

        return relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getId();
    }

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance consultation EF9", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private ResultActions marquerPresence(long sessionId, long etudiantId) throws Exception {
        return mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "%s", "etudiantId": %d }
                        """.formatted(codeDeSession(sessionId), etudiantId)));
    }

    private String codeDeSession(long sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow().getCode();
    }
}
