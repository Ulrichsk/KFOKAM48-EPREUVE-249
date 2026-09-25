package fr.kfokam48.relecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.exercice.ExerciceRepository;
import fr.kfokam48.exercice.StatutExercice;
import fr.kfokam48.session.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP du rendu de relecture (EF10, RG11-RG13, RG21) :
 * {@code POST /api/relectures/{id}} (issue 10).
 *
 * <p>Etudiants utilises : 3 (auteur), 10 (relecteur) et 11 (intrus) de la promotion 1 —
 * les autres identifiants sont deja consommes par les autres classes de test, le contexte
 * Spring et la base H2 etant partages. Chaque test part d'une relecture creee par la voie
 * nominale (session, presence du relecteur, depot de l'auteur, tirage).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RendreRelectureIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 3L;
    private static final long RELECTEUR = 10L;
    private static final long INTRUS = 11L;
    private static final String LIEN = "https://example.invalid/rendus/3-exercice10.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // ------------------------------------------------------------------ cas nominal

    @Test
    @DisplayName("note entiere 0-20 : 200, relecture RENDUE, exercice RELU (EF10, RG12)")
    void rend_une_note_valide() throws Exception {
        long relectureId = creerRelectureParTirage();

        String corps = mockMvc.perform(rendre(relectureId, RELECTEUR)
                        .content("""
                                { "note": 14, "commentaire": "Bon travail." }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relectureId))
                .andExpect(jsonPath("$.exerciceId").isNumber())
                .andExpect(jsonPath("$.statut").value("RENDUE"))
                .andExpect(jsonPath("$.note").value(14))
                .andExpect(jsonPath("$.commentaire").value("Bon travail."))
                .andExpect(jsonPath("$.renduAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode rendue = objectMapper.readTree(corps);
        assertThat(rendue.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "exerciceId", "statut", "note",
                        "commentaire", "renduAt");
        // Anonymat (Q8) : aucun champ ne designe le relecteur.
        assertThat(rendue.has("relecteurId")).isFalse();

        Relecture relecture = relectureRepository.findById(relectureId).orElseThrow();
        assertThat(relecture.getStatut()).isEqualTo(StatutRelecture.RENDUE);
        assertThat(relecture.getNote()).isEqualTo(14);
        assertThat(relecture.getRenduAt()).isNotNull();

        long exerciceId = relecture.getExerciceId();
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.RELU);
    }

    @Test
    @DisplayName("commentaire absent ou vide : l'envoi est accepte, seul le commentaire est facultatif")
    void accepte_un_commentaire_absent_ou_vide() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, RELECTEUR).content("{ \"note\": 20 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(20))
                .andExpect(jsonPath("$.commentaire").doesNotExist());

        long autreId = creerRelectureParTirage();
        mockMvc.perform(rendre(autreId, RELECTEUR).content("{ \"note\": 18, \"commentaire\": \"\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentaire").doesNotExist());
    }

    @Test
    @DisplayName("note 0 et note 20 : les bornes sont incluses (Q9)")
    void accepte_les_bornes_zero_et_vingt() throws Exception {
        long premier = creerRelectureParTirage();
        mockMvc.perform(rendre(premier, RELECTEUR).content("{ \"note\": 0 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(0));

        long second = creerRelectureParTirage();
        mockMvc.perform(rendre(second, RELECTEUR).content("{ \"note\": 20 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(20));
    }

    // ------------------------------------------------------------------ notes refusees

    @Test
    @DisplayName("note absente : 400 NOTE_INVALIDE et rien n'est enregistre")
    void refuse_une_note_absente() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, RELECTEUR).content("{ \"commentaire\": \"sans note\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.findById(relectureId).orElseThrow().getStatut())
                .isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    @Test
    @DisplayName("note decimale (14.5) : 400 NOTE_INVALIDE, conformement au contrat")
    void refuse_une_note_decimale() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, RELECTEUR).content("{ \"note\": 14.5 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.findById(relectureId).orElseThrow().getStatut())
                .isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    @Test
    @DisplayName("note hors bornes (21 puis -1) : 400 NOTE_INVALIDE")
    void refuse_une_note_hors_bornes() throws Exception {
        long premier = creerRelectureParTirage();
        mockMvc.perform(rendre(premier, RELECTEUR).content("{ \"note\": 21 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));

        long second = creerRelectureParTirage();
        mockMvc.perform(rendre(second, RELECTEUR).content("{ \"note\": -1 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("commentaire de plus de 2000 caracteres : 400 VALEUR_INVALIDE (contrat)")
    void refuse_un_commentaire_trop_long() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, RELECTEUR)
                        .content("{ \"note\": 10, \"commentaire\": \"%s\" }"
                                .formatted("x".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALEUR_INVALIDE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.findById(relectureId).orElseThrow().getStatut())
                .isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    // ------------------------------------------------------------------ autorisations

    @Test
    @DisplayName("l'auteur ne peut pas noter son propre exercice : 403 AUTO_EVALUATION_INTERDITE (Q5)")
    void refuse_l_auteur_comme_rendeur() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, AUTEUR).content("{ \"note\": 15 }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_EVALUATION_INTERDITE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.findById(relectureId).orElseThrow().getStatut())
                .isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    @Test
    @DisplayName("un etudiant autre que le relecteur designe est refuse : 403 ACCES_REFUSE")
    void refuse_un_intrus() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, INTRUS).content("{ \"note\": 15 }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(relectureRepository.findById(relectureId).orElseThrow().getStatut())
                .isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    // ------------------------------------------------------------------ etat et 404

    @Test
    @DisplayName("second envoi : 409 RELECTURE_DEJA_RENDUE et note d'origine inchangee (Q15, RG12)")
    void refuse_un_second_envoi_et_conserve_la_note_d_origine() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(rendre(relectureId, RELECTEUR).content("{ \"note\": 12, \"commentaire\": \"premiere\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(rendre(relectureId, RELECTEUR).content("{ \"note\": 20, \"commentaire\": \"correction\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        Relecture relecture = relectureRepository.findById(relectureId).orElseThrow();
        assertThat(relecture.getNote()).isEqualTo(12);
        assertThat(relecture.getCommentaire()).isEqualTo("premiere");
    }

    @Test
    @DisplayName("relecture inconnue : 404 RELECTURE_INTROUVABLE (contrat)")
    void refuse_une_relecture_inconnue() throws Exception {
        mockMvc.perform(rendre(999_999L, RELECTEUR).content("{ \"note\": 10 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INTROUVABLE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("header X-Etudiant-Id absent : 400 CHAMP_MANQUANT (contrat)")
    void exige_le_header_d_identite() throws Exception {
        long relectureId = creerRelectureParTirage();

        mockMvc.perform(post("/api/relectures/{id}", relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": 10 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    // ------------------------------------------------------------------ outils

    /**
     * Cree une relecture par la voie nominale : session ouverte, presence du relecteur,
     * depot de l'auteur, tirage au sort. Renvoie l'identifiant de la relecture.
     */
    private long creerRelectureParTirage() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(sessionRepository.findById(sessionId).orElseThrow().getCode(), RELECTEUR)))
                .andExpect(status().isCreated());

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
                                { "titre": "Seance rendu EF10", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private MockHttpServletRequestBuilder rendre(long relectureId, long etudiantId) {
        return post("/api/relectures/{id}", relectureId)
                .header("X-Etudiant-Id", etudiantId)
                .contentType(MediaType.APPLICATION_JSON);
    }
}
