package fr.kfokam48.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.exercice.ExerciceRepository;
import fr.kfokam48.exercice.StatutExercice;
import fr.kfokam48.relecture.RelectureRepository;
import fr.kfokam48.relecture.RelectureService;
import fr.kfokam48.relecture.DemandeRelecture;
import fr.kfokam48.relecture.RelectureRendue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/sessions/{id}/cloture} (EF14, RG21,
 * decision 7.5) — issue 13.
 *
 * <p>Etudiants utilises : 11 (auteur), 12 (relecteur) — les identifiants 1 a 10 sont
 * deja charges d'activite par les autres classes de test. La clôture est idempotente,
 * ferme les depots et les presences, mais n'interdit jamais le rendu d'une relecture
 * deja assignee (RG21) : ce dernier point est verifie en appelant le vrai service de
 * relecture, sans passer par un endpoint qui n'existe pas.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClotureSessionIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 11L;
    private static final long RELECTEUR = 12L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private RelectureService relectureService;

    // ------------------------------------------------------------------ clôture nominale

    @Test
    @DisplayName("le formateur clôture une session ouverte : 200 avec clotureAt (EF14)")
    void cloture_une_session_ouverte() throws Exception {
        long sessionId = ouvrirSession();

        String corps = mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.clotureAt").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reponse = objectMapper.readTree(corps);
        assertThat(reponse.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "clotureAt");

        LocalDateTime clotureAt = sessionRepository.findById(sessionId).orElseThrow().getClotureAt();
        assertThat(clotureAt).isNotNull();
    }

    @Test
    @DisplayName("recloturer une session deja close : 200 avec la meme date, sans erreur")
    void cloture_est_idempotente() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk());

        // La date renvoyee par la seconde clôture est celle qui vient de la base :
        // elle doit etre identique a la valeur persistee par la premiere (idempotence).
        LocalDateTime attendu = sessionRepository.findById(sessionId).orElseThrow().getClotureAt();

        String seconde = mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.clotureAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(LocalDateTime.parse(objectMapper.readTree(seconde).get("clotureAt").asText()))
                .isEqualTo(attendu);
    }

    @Test
    @DisplayName("session inconnue : 404 SESSION_INTROUVABLE (contrat)")
    void refuse_une_session_inconnue() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/cloture", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INTROUVABLE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ------------------------------------------------------------------ effets de la clôture

    @Test
    @DisplayName("apres clôture : depot refuse 409 SESSION_CLOTUREE (RG21, Q12)")
    void refuse_le_depot_apres_cloture() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, AUTEUR, "https://example.invalid/rendus/11-ex13.pdf")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("apres clôture : presence refusee 409 SESSION_CLOTUREE (RG21, Q3)")
    void refuse_la_presence_apres_cloture() throws Exception {
        long sessionId = ouvrirSession();
        String code = codeDe(sessionId);

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(code, RELECTEUR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("relecture assignee avant la clôture : le rendu reste possible (RG21)")
    void laisse_le_rendu_possible_apres_cloture() throws Exception {
        RelectureCreee relecture = creerRelectureParTirage();

        mockMvc.perform(post("/api/sessions/{id}/cloture", relecture.sessionId()))
                .andExpect(status().isOk());

        // RG21 : la clôture ferme les depots et les presences, pas les notes. Le
        // rendu passe par le vrai service, sans contournement de la regle metier.
        RelectureRendue rendue = relectureService.rendreRelecture(
                relecture.relectureId(), RELECTEUR, new DemandeRelecture(16, "Encore un effort."));
        assertThat(rendue.statut()).isEqualTo(fr.kfokam48.relecture.StatutRelecture.RENDUE);

        assertThat(relectureRepository.findById(relecture.relectureId()).orElseThrow().getNote())
                .isEqualTo(16);
    }

    @Test
    @DisplayName("l'exercice d'une relecture rendue apres clôture passe a RELU (RG12)")
    void passe_l_exercice_a_relu_apres_cloture() throws Exception {
        RelectureCreee relecture = creerRelectureParTirage();

        mockMvc.perform(post("/api/sessions/{id}/cloture", relecture.sessionId()))
                .andExpect(status().isOk());

        relectureService.rendreRelecture(relecture.relectureId(), RELECTEUR, new DemandeRelecture(12, null));

        long exerciceId = relectureRepository.findById(relecture.relectureId()).orElseThrow().getExerciceId();
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.RELU);
    }

    // ------------------------------------------------------------------ outils

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance cloture EF14", "promotionId": %d }
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

    /** Identifiants de la relecture creee et de la session qui la porte. */
    private record RelectureCreee(long relectureId, long sessionId) {
    }

    /**
     * Cree une relecture par la voie nominale AVANT la clôture : presence du
     * relecteur, depot de l'auteur, tirage au sort.
     */
    private RelectureCreee creerRelectureParTirage() throws Exception {
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
                                """.formatted(sessionId, AUTEUR, "https://example.invalid/rendus/11-ex13b.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long exerciceId = objectMapper.readTree(corps).get("id").asLong();

        long relectureId = relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getId();
        return new RelectureCreee(relectureId, sessionId);
    }
}
