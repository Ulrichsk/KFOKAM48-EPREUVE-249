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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code GET /api/etudiants/{id}/exercices} (EF11, RG20) —
 * issue 11.
 *
 * <p>Etudiants utilises : 6 (auteur), 11 (relecteur), 8 (intrus) de la promotion 1.
 * Le scenario construit deux exercices de l'auteur : le premier relu avec la note 15
 * et un commentaire, le second reste en attente — pour verifier que la note n'apparait
 * qu'apres rendu, et qu'aucune reponse ne trahit le relecteur (Q8, RG20).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConsultationNoteIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 6L;
    private static final long RELECTEUR = 11L;
    private static final long INTRUS = 8L;

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

    @Test
    @DisplayName("l'auteur relu voit sa note et son commentaire, sans identite de relecteur (Q8, RG20)")
    void affiche_la_note_et_le_commentaire_sans_identite_relecteur() throws Exception {
        long exerciceRelu = creerExerciceEtRendreLaNote();
        creerExerciceEnAttente();

        String corps = mockMvc.perform(get("/api/etudiants/{id}/exercices", AUTEUR)
                        .header("X-Etudiant-Id", AUTEUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode exercices = objectMapper.readTree(corps);
        assertThat(exercices).hasSize(2);

        JsonNode relu = exerciceDe(exercices, exerciceRelu);
        assertThat(relu.get("note").asInt()).isEqualTo(15);
        assertThat(relu.get("commentaire").asText()).isEqualTo("Bon decoupage, arguments clairs.");
        assertThat(relu.get("statut").asText()).isEqualTo("RELU");
        assertThat(relu.get("titreSession").asText()).isNotEmpty();

        // Champs du contrat, et strictement rien d'autre.
        assertThat(relu.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "sessionId", "titreSession", "statut",
                        "note", "commentaire");
        // Anonymat (RG20) : aucune cle ne designe le relecteur.
        assertThat(relu.has("relecteurId")).isFalse();
        assertThat(relu.has("relecteur")).isFalse();
        assertThat(relu.has("assigneAt")).isFalse();
        assertThat(corps.contains("relecteur")).isFalse();
    }

    @Test
    @DisplayName("un exercice en attente n'affiche ni note ni commentaire (Q8, Q15)")
    void masque_la_note_tant_que_la_relecture_n_est_pas_rendue() throws Exception {
        creerExerciceEtRendreLaNote();
        long exerciceEnAttente = creerExerciceEnAttente();

        String corps = mockMvc.perform(get("/api/etudiants/{id}/exercices", AUTEUR)
                        .header("X-Etudiant-Id", AUTEUR))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode exercices = objectMapper.readTree(corps);
        for (JsonNode exercice : exercices) {
            if (exercice.get("id").asLong() != exerciceEnAttente) {
                continue;
            }
            // Relecture EN_ATTENTE : note et commentaire absents, statut visible.
            assertThat(exercice.get("note").isNull()).isTrue();
            assertThat(exercice.get("commentaire").isNull()).isTrue();
            assertThat(exercice.get("statut").asText()).isEqualTo("EN_ATTENTE");
        }
    }

    @Test
    @DisplayName("un autre etudiant ne consulte pas la liste : 403 ACCES_REFUSE (RG20)")
    void refuse_la_consultation_des_exercices_d_un_autre() throws Exception {
        creerExerciceEtRendreLaNote();

        mockMvc.perform(get("/api/etudiants/{id}/exercices", AUTEUR)
                        .header("X-Etudiant-Id", INTRUS))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("etudiant inconnu : 404 ETUDIANT_INCONNU (contrat)")
    void refuse_un_etudiant_inconnu() throws Exception {
        mockMvc.perform(get("/api/etudiants/{id}/exercices", 999_999L)
                        .header("X-Etudiant-Id", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("header X-Etudiant-Id absent : 400 CHAMP_MANQUANT (contrat)")
    void exige_le_header_d_identite() throws Exception {
        mockMvc.perform(get("/api/etudiants/{id}/exercices", AUTEUR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    // ------------------------------------------------------------------ outils

    /** Cree un exercice relu : session, presence du relecteur, depot, tirage, note 15. */
    private long creerExerciceEtRendreLaNote() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(post("/api/relectures/{id}", relectureId)
                        .header("X-Etudiant-Id", RELECTEUR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": 15, \"commentaire\": \"Bon decoupage, arguments clairs.\" }"))
                .andExpect(status().isOk());

        return relectureRepository.findById(relectureId).orElseThrow().getExerciceId();
    }

    /** Cree un exercice dont la relecture reste EN_ATTENTE : renvoie l'identifiant de l'exercice. */
    private long creerExerciceEnAttente() throws Exception {
        long relectureId = creerRelectureParTirage();
        return relectureRepository.findById(relectureId).orElseThrow().getExerciceId();
    }

    /** Session + presence du relecteur + depot de l'auteur + tirage : renvoie la relecture. */
    private long creerRelectureParTirage() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(codeDe(sessionId), RELECTEUR)))
                .andExpect(status().isCreated());

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, AUTEUR,
                                "https://example.invalid/rendus/6-ex11-" + System.nanoTime() + ".pdf")))
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
                                { "titre": "Seance consultation note EF11", "promotionId": %d }
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

    private JsonNode exerciceDe(JsonNode exercices, long exerciceId) {
        for (JsonNode exercice : exercices) {
            if (exercice.get("id").asLong() == exerciceId) {
                return exercice;
            }
        }
        throw new AssertionError("Aucun exercice " + exerciceId + " dans la reponse");
    }
}
