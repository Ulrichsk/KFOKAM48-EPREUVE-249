package fr.kfokam48.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.relecture.AssignePar;
import fr.kfokam48.relecture.RelectureRepository;
import fr.kfokam48.session.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du changement de besoin de l'enveloppe etape 3 (issue 16) :
 * le formateur designe un relecteur aux exercices restes {@code SANS_RELECTEUR}
 * directement depuis l'ecran du tableau de bord.
 *
 * <p>Le parcours complet du client est exerce : lire le tableau, reperer
 * {@code exercicesSansRelecteur} sur la ligne de l'auteur, designer un relecteur par
 * l'endpoint existant {@code POST /api/exercices/{id}/relecteur}, relire le tableau —
 * l'exercice en est sorti. L'ecran, lui, est verifie par la compilation TypeScript
 * stricte et le build Vite ; c'est ici que le contrat est prouve.</p>
 *
 * <p>Etudiants utilises (promotion 1, hors etudiants 5-10 portes de compteurs de
 * blocage par d'autres classes) : auteur = 4, relecteur = 12, intrus promotion 2 = 13.
 * Le scenario ouvre ses propres sessions : aucun interference avec l'historique des
 * autres classes.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class DesignationDepuisTableauIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 4L;
    private static final long RELECTEUR = 12L;
    private static final long INTRUS_PROMO_2 = 13L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private AuditAssignationManuelleRepository auditRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("parcours complet : tableau -> designation -> tableau, avec trace d'audit (RG23)")
    void designe_un_relecteur_depuis_le_tableau_et_trace_l_acte() throws Exception {
        long exerciceId = deposerSansRelecteur();
        long exerciceIdRef = exerciceId; // lisibilite des assertions de tableau

        // 1. Avant designation : la ligne de l'auteur expose l'exercice bloque.
        assertThat(idsSansRelecteurDe(AUTEUR)).contains(exerciceIdRef);

        // 2. Le formateur designe un relecteur de la promotion (autre que l'auteur).
        assigner(exerciceId, RELECTEUR)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.assignePar").value("FORMATEUR"));
        long relectureId = lireRelectureId(exerciceId);

        // 3. Apres designation : l'exercice a quitte la liste des sans-relecteur,
        //    il est desormais compte dans les relectures en attente du relecteur.
        assertThat(idsSansRelecteurDe(AUTEUR)).doesNotContain(exerciceIdRef);
        JsonNode ligneRelecteur = ligneDe(RELECTEUR);
        assertThat(ligneRelecteur.get("relecturesEnAttente").asLong()).isPositive();

        // 4. La relecture existe en base, portee par la voie FORMATEUR.
        var relecture = relectureRepository.findById(relectureId).orElseThrow();
        assertThat(relecture.getAssignePar()).isEqualTo(AssignePar.FORMATEUR);
        assertThat(relecture.getRelecteurId()).isEqualTo(RELECTEUR);
        assertThat(relecture.getLienConsulteAt()).isNull();
        assertThat(relecture.getNote()).isNull();

        // 5. RG23 : la designation reussie a laisse exactement une trace d'audit.
        var audit = auditRepository.findByExercice_Id(exerciceId).orElseThrow();
        assertThat(audit.getExercice().getId()).isEqualTo(exerciceId);
        assertThat(audit.getRelecteur().getId()).isEqualTo(RELECTEUR);
        assertThat(audit.getAssigneAt()).isNotNull();

        // 6. Determinisme : purge de la trace de fin de test, ce scenario ne laisse
        //    rien qui puisse influencer les autres classes (lecture seule du tableau).
        jdbcTemplate.update("delete from audits_assignation_manuelle where exercice_id = ?", exerciceId);
    }

    @Test
    @DisplayName("l'auteur propose par l'ecran est refuse : 403 AUTO_EVALUATION_INTERDITE, aucune trace")
    void refuse_la_designation_de_l_auteur_meme_par_l_ecran() throws Exception {
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, AUTEUR)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_EVALUATION_INTERDITE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Aucun effet de bord : l'exercice reste SANS_RELECTEUR, aucune trace d'audit.
        assertThat(idsSansRelecteurDe(AUTEUR)).contains(exerciceId);
        assertThat(auditRepository.findByExercice_Id(exerciceId)).isEmpty();
    }

    @Test
    @DisplayName("un etudiant d'une autre promotion est refuse : 403 ACCES_REFUSE (RG19)")
    void refuse_un_relecteur_hors_promotion() throws Exception {
        long exerciceId = deposerSansRelecteur();

        assigner(exerciceId, INTRUS_PROMO_2)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(auditRepository.findByExercice_Id(exerciceId)).isEmpty();
    }

    // ------------------------------------------------------------------ outils

    private ResultActions assigner(long exerciceId, long relecteurId) throws Exception {
        return mockMvc.perform(post("/api/exercices/{id}/relecteur", exerciceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "relecteurId": %d }
                        """.formatted(relecteurId)));
    }

    /** Depot dans une session ou personne n'est present : l'exercice reste SANS_RELECTEUR (RG10). */
    private long deposerSansRelecteur() throws Exception {
        long sessionId = ouvrirSession();

        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "https://example.invalid/rendus/4-ex16.pdf" }
                                """.formatted(sessionId, AUTEUR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SANS_RELECTEUR"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    /** Les identifiants exposes par la ligne du tableau de l'etudiant (champ issue 16). */
    private java.util.List<Long> idsSansRelecteurDe(long etudiantId) throws Exception {
        JsonNode noeud = ligneDe(etudiantId).get("exercicesSansRelecteur");
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (JsonNode id : noeud) {
            ids.add(id.asLong());
        }
        return ids;
    }

    private JsonNode ligneDe(long etudiantId) throws Exception {
        String corps = mockMvc.perform(get("/api/tableau").param("promotionId", String.valueOf(PROMOTION_1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (JsonNode ligne : objectMapper.readTree(corps)) {
            if (ligne.get("etudiantId").asLong() == etudiantId) {
                return ligne;
            }
        }
        throw new AssertionError("Aucune ligne du tableau pour l'etudiant " + etudiantId);
    }

    private long lireRelectureId(long exerciceId) {
        return relectureRepository.findByExercice_Id(exerciceId).orElseThrow().getId();
    }

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance designation issue 16", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }
}
