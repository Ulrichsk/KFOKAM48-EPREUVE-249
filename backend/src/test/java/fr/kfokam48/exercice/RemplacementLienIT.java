package fr.kfokam48.exercice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.relecture.RelectureRepository;
import fr.kfokam48.session.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code PUT /api/exercices/{id}} (EF6, RG16, RG21) —
 * issue 06.
 *
 * <p>Etudiants utilises : 4 (auteur), 12 (relecteur) et 5 (intrus) de la promotion 1.
 * Chaque test ouvre ses propres sessions : l'unicite d'exercice portant sur le couple
 * (session, etudiant), l'historique laisse par les autres classes sur d'autres sessions
 * n'interfere pas. Le relecteur marque sa presence avec le bon code, ce qui remet son
 * compteur d'echecs a zero : aucun blocage 429 ne peut survenir.</p>
 *
 * <p>Le point cle de Q13 : c'est la consultation du lien par le relecteur (issue 09,
 * {@code lien_consulte_at}) qui fige le remplacement — jamais la seule existence d'une
 * relecture en attente.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RemplacementLienIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 4L;
    private static final long RELECTEUR = 12L;
    private static final long INTRUS = 5L;
    private static final String LIEN_INITIAL = "https://example.invalid/rendus/4-ex6-v1.pdf";
    private static final String LIEN_CORRIGE = "https://example.invalid/rendus/4-ex6-v2.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // ------------------------------------------------------------------ cas nominal

    @Test
    @DisplayName("l'auteur remplace son lien avant toute consultation : 200, lien corrige (Q13)")
    void remplace_son_lien_avant_toute_consultation() throws Exception {
        long exerciceId = creerExerciceAvecRelecteur();

        String corps = remplacer(exerciceId, AUTEUR, LIEN_CORRIGE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciceId))
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.lien").value(LIEN_CORRIGE))
                .andExpect(jsonPath("$.relectureCommencee").value(false))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detail = objectMapper.readTree(corps);
        assertThat(detail.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "sessionId", "statut", "lien", "relectureCommencee");

        Exercice exercice = exerciceRepository.findById(exerciceId).orElseThrow();
        assertThat(exercice.getLien()).isEqualTo(LIEN_CORRIGE);
        assertThat(relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getLienConsulteAt())
                .isNull();
    }

    @Test
    @DisplayName("un exercice sans relecteur peut voir son lien remplace : aucune relecture n'a commence")
    void remplace_le_lien_d_un_exercice_sans_relecteur() throws Exception {
        long exerciceId = creerExerciceSansRelecteur();

        remplacer(exerciceId, AUTEUR, LIEN_CORRIGE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SANS_RELECTEUR"))
                .andExpect(jsonPath("$.relectureCommencee").value(false));

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getLien())
                .isEqualTo(LIEN_CORRIGE);
    }

    // ------------------------------------------------------------------ lien fige (Q13, RG16)

    @Test
    @DisplayName("apres consultation du lien par le relecteur : 409 RELECTURE_COMMENCEE, ancien lien intact")
    void fige_le_lien_apres_consultation_par_le_relecteur() throws Exception {
        long exerciceId = creerExerciceAvecRelecteur();
        long relectureId = relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getId();

        // Le relecteur ouvre le lien : la premiere consultation horodate l'instant
        // qui fige le remplacement (issue 09, Q13).
        mockMvc.perform(get("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", RELECTEUR))
                .andExpect(status().isOk());

        remplacer(exerciceId, AUTEUR, LIEN_CORRIGE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_COMMENCEE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // L'ancien lien reste en place : aucune modification partielle n'a ete ecrite.
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getLien())
                .isEqualTo(LIEN_INITIAL);
    }

    // ------------------------------------------------------------------ autorisations

    @Test
    @DisplayName("un etudiant autre que l'auteur est refuse : 403 ACCES_REFUSE, lien intact")
    void refuse_un_appelant_qui_n_est_pas_l_auteur() throws Exception {
        long exerciceId = creerExerciceAvecRelecteur();

        remplacer(exerciceId, INTRUS, LIEN_CORRIGE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getLien())
                .isEqualTo(LIEN_INITIAL);
    }

    // ------------------------------------------------------------------ lien invalide

    @Test
    @DisplayName("nouveau lien mal forme : 400 LIEN_INVALIDE, ancien lien conserve (RG22)")
    void refuse_un_lien_invalide_et_conserve_l_ancien() throws Exception {
        long exerciceId = creerExerciceAvecRelecteur();

        remplacer(exerciceId, AUTEUR, "ftp://example.invalid/pas-bon")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getLien())
                .isEqualTo(LIEN_INITIAL);
    }

    // ------------------------------------------------------------------ session close

    @Test
    @DisplayName("apres clôture de la session : 409 SESSION_CLOTUREE (RG21)")
    void refuse_le_remplacement_apres_cloture() throws Exception {
        long exerciceId = creerExerciceAvecRelecteur();
        long sessionId = exerciceRepository.findById(exerciceId).orElseThrow().getSessionId();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk());

        remplacer(exerciceId, AUTEUR, LIEN_CORRIGE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getLien())
                .isEqualTo(LIEN_INITIAL);
    }

    // ------------------------------------------------------------------ erreurs de forme et 404

    @Test
    @DisplayName("exercice inconnu : 404 EXERCICE_INTROUVABLE (contrat)")
    void refuse_un_exercice_inconnu() throws Exception {
        remplacer(999_999L, AUTEUR, LIEN_CORRIGE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INTROUVABLE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("header X-Etudiant-Id absent : 400 CHAMP_MANQUANT (contrat)")
    void exige_le_header_d_identite() throws Exception {
        mockMvc.perform(put("/api/exercices/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lien": "%s" }
                                """.formatted(LIEN_CORRIGE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    // ------------------------------------------------------------------ outils

    private ResultActions remplacer(long exerciceId, long etudiantId, String lien) throws Exception {
        return mockMvc.perform(put("/api/exercices/{id}", exerciceId)
                .header("X-Etudiant-Id", etudiantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "lien": "%s" }
                        """.formatted(lien)));
    }

    /**
     * Cree un exercice avec relecteur par la voie nominale : session, presence du
     * relecteur (bon code), depot de l'auteur, tirage au sort.
     */
    private long creerExerciceAvecRelecteur() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(codeDe(sessionId), RELECTEUR)))
                .andExpect(status().isCreated());

        return deposer(sessionId, AUTEUR, LIEN_INITIAL, "EN_ATTENTE");
    }

    /** Cree un exercice dans une session ou personne n'est present : SANS_RELECTEUR (RG10). */
    private long creerExerciceSansRelecteur() throws Exception {
        long sessionId = ouvrirSession();
        return deposer(sessionId, AUTEUR, LIEN_INITIAL, "SANS_RELECTEUR");
    }

    private long deposer(long sessionId, long etudiantId, String lien, String statutAttendu) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, etudiantId, lien)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value(statutAttendu))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance remplacement EF6", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private String codeDe(long sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow().getCode();
    }
}
