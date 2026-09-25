package fr.kfokam48.tableau;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.relecture.RelectureRepository;
import fr.kfokam48.session.SessionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code GET /api/tableau?promotionId=} (EF12, RG17, RG18,
 * Q11, Q14, Q16) — issue 12.
 *
 * <p><b>Choix des etudiants.</b> Le contexte Spring et la base H2 sont partages entre
 * toutes les classes de test : beaucoup d'etudiants de la promotion 1 portent deja des
 * presences, des depots ou des compteurs d'echecs poses par les autres classes. Le
 * scenario utilise les trois etudiants a etat vierge ou parfaitement connu :</p>
 * <ul>
 *   <li><b>9</b> n'a jamais depose, jamais recu de note, jamais ete designe : il sert
 *       d'auteur relu, avec des comptes exacts ;</li>
 *   <li><b>7</b> n'a jamais reussi de presence (ses echecs de code lui ont laisse un
 *       compteur, que le scenario purge pour rester deterministe) : il sert de
 *       relecteur, et sa presence est la seule de son compte ;</li>
 *   <li><b>2</b> a depose mais n'a ni presence, ni note recue, ni relecture en attente :
 *       sa ligne prouve qu'un etudiant peu actif reste visible avec ses zeros (Q16).</li>
 * </ul>
 *
 * <p>L'etat est construit par l'API seule, une seule fois pour la classe, puis chaque
 * test lit le tableau a sa facon.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableauControllerIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR_RELU = 9L;
    private static final long RELECTEUR = 7L;
    private static final long PEU_ACTIF = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void construireLEtatDeDemonstration() throws Exception {
        // Le compteur d'echecs de 7 vient d'une autre classe de test : purge pour que
        // la presence de ce scenario soit la seule ecriture de son compte (determinisme).
        jdbcTemplate.update("delete from tentatives_code where etudiant_id = ?", RELECTEUR);

        // Session 1 : presence du relecteur 7, depot de l'auteur 9, tirage, note 14.
        long session1 = ouvrirSession("Seance tableau - note rendue");
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(sessionRepository.findById(session1).orElseThrow().getCode(), RELECTEUR)))
                .andExpect(status().isCreated());

        String depot1 = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(session1, AUTEUR_RELU, "https://example.invalid/rendus/9-ex12a.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long exercice1 = objectMapper.readTree(depot1).get("id").asLong();
        long relecture1 = relectureRepository.findByExercice_Id(exercice1).orElseThrow().getId();

        mockMvc.perform(post("/api/relectures/{id}", relecture1)
                        .header("X-Etudiant-Id", RELECTEUR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": 14, \"commentaire\": \"Tres bien.\" }"))
                .andExpect(status().isOk());

        // Session 2 : personne n'est present. Le second depot de l'auteur reste
        // SANS_RELECTEUR : il compte pour exercicesDeposes et jamais pour une moyenne.
        long session2 = ouvrirSession("Seance tableau - sans relecteur");
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(session2, AUTEUR_RELU, "https://example.invalid/rendus/9-ex12b.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SANS_RELECTEUR"));
    }

    @Test
    @DisplayName("une ligne par etudiant de la promotion, champ exact du contrat (Q16)")
    void affiche_une_ligne_par_etudiant_avec_les_champs_du_contrat() throws Exception {
        JsonNode lignes = lignesDeLaPromotion();

        assertThat(lignes).hasSize(12); // la promotion 1 compte 12 etudiants (V2)

        // Champs du contrat, et strictement rien d'autre.
        assertThat(lignes.get(0).fieldNames()).toIterable()
                .containsExactlyInAnyOrder("etudiantId", "nom", "prenom", "presences",
                        "presencesFormateur", "exercicesDeposes", "moyenne", "relecturesEnAttente");
    }

    @Test
    @DisplayName("l'auteur relu : deux depots, une moyenne de 14.0 venue de l'API, rien en attente")
    void affiche_les_comptes_de_l_auteur_relu() throws Exception {
        JsonNode ligne = ligneDe(AUTEUR_RELU);

        assertThat(ligne.get("presences").asLong()).isZero();
        assertThat(ligne.get("exercicesDeposes").asLong()).isEqualTo(2);
        assertThat(ligne.get("moyenne").asDouble()).isEqualTo(14.0);
        assertThat(ligne.get("relecturesEnAttente").asLong()).isZero();
    }

    @Test
    @DisplayName("le relecteur : une presence, aucun depot, aucune note recue, rien en attente (RG18)")
    void affiche_les_comptes_du_relecteur() throws Exception {
        JsonNode ligne = ligneDe(RELECTEUR);

        assertThat(ligne.get("presences").asLong()).isEqualTo(1);
        assertThat(ligne.get("presencesFormateur").asLong()).isZero();
        assertThat(ligne.get("exercicesDeposes").asLong()).isZero();
        assertThat(ligne.get("moyenne").asDouble()).isZero();
        assertThat(ligne.get("relecturesEnAttente").asLong()).isZero();
    }

    @Test
    @DisplayName("un etudiant peu actif reste visible, avec ses zeros (Q16, RG18)")
    void affiche_la_ligne_d_un_etudiant_peu_actif() throws Exception {
        JsonNode ligne = ligneDe(PEU_ACTIF);

        assertThat(ligne.get("presences").asLong()).isZero();
        // L'etudiant 2 a depose dans d'autres classes de test : au moins un depot,
        // mais aucune note recue ni relecture en attente.
        assertThat(ligne.get("exercicesDeposes").asLong()).isPositive();
        assertThat(ligne.get("moyenne").asDouble()).isZero();
        assertThat(ligne.get("relecturesEnAttente").asLong()).isZero();
    }

    @Test
    @DisplayName("promotion inconnue : 404 PROMOTION_INCONNUE (contrat impose)")
    void refuse_une_promotion_inconnue() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("parametre promotionId absent : 400 CHAMP_MANQUANT (ENF4)")
    void exige_le_parametre_promotion() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ------------------------------------------------------------------ outils

    private JsonNode lignesDeLaPromotion() throws Exception {
        String corps = mockMvc.perform(get("/api/tableau").param("promotionId", String.valueOf(PROMOTION_1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps);
    }

    private JsonNode ligneDe(long etudiantId) throws Exception {
        for (JsonNode ligne : lignesDeLaPromotion()) {
            if (ligne.get("etudiantId").asLong() == etudiantId) {
                return ligne;
            }
        }
        throw new AssertionError("Aucune ligne du tableau pour l'etudiant " + etudiantId);
    }

    private long ouvrirSession(String titre) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "%s", "promotionId": %d }
                                """.formatted(titre, PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }
}
