package fr.kfokam48.relecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.exercice.ExerciceRepository;
import fr.kfokam48.exercice.StatutExercice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/exercices/{id}/relecteur} (EF8, RG6, RG10,
 * RG14, decision 7.4).
 *
 * <p>Chaque test part d'un exercice reellement reste {@code SANS_RELECTEUR} : la session
 * est ouverte par l'API puis l'exercice est depose <b>sans aucune presence</b>, ce qui est
 * exactement la situation que l'endpoint doit debloquer (§7.4). Rien n'est fabrique
 * directement en base, sauf le cas « deja relu » explique plus bas.</p>
 *
 * <p>Etudiants utilises : 1 (auteur), 6 (relecteur) et 7 (second relecteur) de la
 * promotion 1, 14 (promotion 2) pour le controle RG19 — les autres sont deja consommes par
 * les autres classes de test, le contexte Spring et la base H2 etant partages.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AssignationRelecteurIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 1L;
    private static final long RELECTEUR = 6L;
    private static final long AUTRE_ETUDIANT = 7L;
    private static final long ETUDIANT_PROMOTION_2 = 14L;
    private static final String LIEN = "https://example.invalid/rendus/1-exercice4.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------ nominal

    @Test
    @DisplayName("le formateur designe un relecteur : 201, EN_ATTENTE et assignePar FORMATEUR (EF8, RG10)")
    void assigne_un_relecteur_valide() throws Exception {
        long exerciceId = deposerSansRelecteur();

        String corps = assigner(exerciceId, RELECTEUR)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.exerciceId").value(exerciceId))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.assignePar").value("FORMATEUR"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(corps);
        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "exerciceId", "statut", "assignePar");

        Relecture relecture = relectureRepository.findByExercice_Id(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteurId()).isEqualTo(RELECTEUR);
        assertThat(relecture.getStatut()).isEqualTo(StatutRelecture.EN_ATTENTE);
        assertThat(relecture.getAssignePar()).isEqualTo(AssignePar.FORMATEUR);
        // Q11 : la relecture attend sa note, elle n'est pas encore rendue.
        assertThat(relecture.getNote()).isNull();
        assertThat(relecture.getRenduAt()).isNull();
        assertThat(relecture.getLienConsulteAt()).isNull();

        // L'exercice a bien quitte SANS_RELECTEUR (RG10).
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.EN_ATTENTE);
        // RG7 : toujours un seul relecteur.
        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("le relecteur designe n'a pas besoin d'etre present a la session (decision 7.4)")
    void assigne_un_relecteur_absent_a_la_session() throws Exception {
        // Aucune presence n'est marquee : le tirage n'avait personne, et pourtant
        // l'assignation manuelle doit aboutir — c'est toute la raison d'etre de l'endpoint.
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, RELECTEUR).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ refus d'autorisation

    @Test
    @DisplayName("l'auteur ne peut jamais etre designe relecteur : 403 AUTO_EVALUATION_INTERDITE (Q5, RG6)")
    void refuse_l_auteur_comme_relecteur() throws Exception {
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, AUTEUR)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_EVALUATION_INTERDITE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // L'exercice reste a debloquer, aucune relecture n'a ete creee.
        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.SANS_RELECTEUR);
    }

    @Test
    @DisplayName("un relecteur d'une autre promotion est refuse : 403 ACCES_REFUSE (RG19)")
    void refuse_un_relecteur_d_une_autre_promotion() throws Exception {
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, ETUDIANT_PROMOTION_2)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
    }

    // ------------------------------------------------------------------ refus d'etat

    @Test
    @DisplayName("un exercice deja pourvu d'un relecteur est refuse : 409 RELECTURE_DEJA_ASSIGNEE (Q6, RG7)")
    void refuse_un_exercice_deja_assigne() throws Exception {
        long exerciceId = deposerSansRelecteur();
        assigner(exerciceId, RELECTEUR).andExpect(status().isCreated());

        assigner(exerciceId, AUTRE_ETUDIANT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_ASSIGNEE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Le relecteur d'origine est conserve : aucun changement de relecteur possible.
        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isEqualTo(1L);
        assertThat(relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getRelecteurId())
                .isEqualTo(RELECTEUR);
    }

    @Test
    @DisplayName("un exercice deja relu est refuse : 409 RELECTURE_DEJA_RENDUE (Q15, RG12)")
    void refuse_un_exercice_deja_relu() throws Exception {
        long exerciceId = deposerSansRelecteur();
        long relectureId = assigner(exerciceId, RELECTEUR, status().isCreated());

        // L'endpoint de rendu appartient a l'issue 10 : en attendant, la ligne est amenee
        // a l'etat RENDUE directement en base, en respectant les contraintes CHECK du
        // schema (statut autorise, note entre 0 et 20). C'est le seul moyen de verifier
        // des aujourd'hui le refus prevu par le contrat pour ce cas.
        marquerRendue(relectureId, 15);

        assigner(exerciceId, AUTRE_ETUDIANT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isEqualTo(1L);
    }

    // ------------------------------------------------------------------ erreurs de forme et 404

    @Test
    @DisplayName("un corps sans relecteurId renvoie 400 CHAMP_MANQUANT (contrat)")
    void refuse_un_relecteur_non_precise() throws Exception {
        long exerciceId = deposerSansRelecteur();

        mockMvc.perform(post("/api/exercices/{id}/relecteur", exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
    }

    @Test
    @DisplayName("un exercice inconnu renvoie 404 EXERCICE_INTROUVABLE (contrat)")
    void refuse_un_exercice_inconnu() throws Exception {
        assigner(999999L, RELECTEUR)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INTROUVABLE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un relecteur inconnu renvoie 404 ETUDIANT_INCONNU (contrat)")
    void refuse_un_relecteur_inconnu() throws Exception {
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
    }

    // ------------------------------------------------------------------ outils

    /** Ouvre une session de la promotion 1 et renvoie son identifiant. */
    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance assignation", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    /**
     * Depose un exercice dans une session ou personne n'est present : le tirage au sort
     * n'a donc aucun candidat et l'exercice reste {@code SANS_RELECTEUR} (RG10).
     */
    private long deposerSansRelecteur() throws Exception {
        long sessionId = ouvrirSession();

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, AUTEUR, LIEN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SANS_RELECTEUR"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private ResultActions assigner(long exerciceId, Long relecteurId) throws Exception {
        return mockMvc.perform(post("/api/exercices/{id}/relecteur", exerciceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "relecteurId": %s }
                        """.formatted(relecteurId)));
    }

    /** Variante qui verifie le statut HTTP et renvoie l'identifiant de la relecture creee. */
    private long assigner(long exerciceId, Long relecteurId, ResultMatcher statutAttendu) throws Exception {
        String corps = assigner(exerciceId, relecteurId)
                .andExpect(statutAttendu)
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private void marquerRendue(long relectureId, int note) {
        jdbcTemplate.update(
                "update relectures set statut = ?, note = ?, commentaire = ?, rendu_at = ? where id = ?",
                "RENDUE",
                note,
                "Rendu depuis le test d'assignation.",
                Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)),
                relectureId);
    }
}
