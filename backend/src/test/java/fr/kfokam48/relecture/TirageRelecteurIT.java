package fr.kfokam48.relecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kfokam48.presence.Presence;
import fr.kfokam48.presence.PresenceRepository;
import fr.kfokam48.presence.SourcePresence;
import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.referentiel.EtudiantRepository;
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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du tirage au sort du relecteur (EF7, RG6 a RG10).
 *
 * <p>Ces tests passent par l'API de depot, donc par la vraie transaction : c'est la seule
 * facon de verifier que le tirage a bien lieu <b>au depot</b> (RG9) et que le statut
 * renvoye correspond a son issue.</p>
 *
 * <p>Les presences sont inserees directement en base : cela permet de controler exactement
 * qui est present, et de fabriquer le cas d'un etudiant ajoute a la main par le formateur
 * (source {@code FORMATEUR}, Q14) sans dependre de l'endpoint correspondant.</p>
 *
 * <p>Etudiants utilises : 3 (auteur) et 4, 11, 12 (candidats) de la promotion 1 — les
 * autres sont deja consommes par les autres classes de test, et le contexte Spring ainsi
 * que la base H2 sont partages.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class TirageRelecteurIT {

    private static final long PROMOTION_1 = 1L;
    private static final long AUTEUR = 3L;
    private static final long CANDIDAT_1 = 4L;
    private static final long CANDIDAT_2 = 11L;
    private static final long CANDIDAT_3 = 12L;
    private static final String LIEN = "https://example.invalid/rendus/3-exercice4.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelectureRepository relectureRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Test
    @DisplayName("un relecteur est tire parmi les presents, jamais l'auteur, et un seul (RG7, RG8, RG6)")
    void un_relecteur_est_tire_parmi_les_presents_hors_auteur() throws Exception {
        long sessionId = ouvrirSession();
        marquerPresent(sessionId, AUTEUR, SourcePresence.ETUDIANT);
        marquerPresent(sessionId, CANDIDAT_1, SourcePresence.ETUDIANT);
        marquerPresent(sessionId, CANDIDAT_2, SourcePresence.ETUDIANT);

        long exerciceId = deposerExercice(sessionId, AUTEUR, "EN_ATTENTE");

        Relecture relecture = relectureRepository.findByExercice_Id(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteurId()).isIn(CANDIDAT_1, CANDIDAT_2);
        assertThat(relecture.getRelecteurId()).isNotEqualTo(AUTEUR);
        assertThat(relecture.getStatut()).isEqualTo(StatutRelecture.EN_ATTENTE);
        assertThat(relecture.getAssignePar()).isEqualTo(AssignePar.SYSTEME);
        assertThat(relecture.getNote()).isNull();
        assertThat(relecture.getRenduAt()).isNull();
        // RG7 : un seul relecteur par exercice.
        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("aucun present : l'exercice reste sans relecteur (RG10)")
    void aucun_present_donne_un_exercice_sans_relecteur() throws Exception {
        long sessionId = ouvrirSession();

        long exerciceId = deposerExercice(sessionId, AUTEUR, "SANS_RELECTEUR");

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
    }

    @Test
    @DisplayName("l'auteur seul present ne peut jamais etre son propre relecteur (Q5, RG6)")
    void l_auteur_seul_present_ne_peut_pas_se_relire() throws Exception {
        long sessionId = ouvrirSession();
        marquerPresent(sessionId, AUTEUR, SourcePresence.ETUDIANT);

        long exerciceId = deposerExercice(sessionId, AUTEUR, "SANS_RELECTEUR");

        assertThat(relectureRepository.countByExercice_Id(exerciceId)).isZero();
    }

    @Test
    @DisplayName("une presence ajoutee par le formateur rend eligible au tirage (Q14, decision 7.2)")
    void une_presence_ajoutee_par_le_formateur_rend_eligible() throws Exception {
        long sessionId = ouvrirSession();
        marquerPresent(sessionId, AUTEUR, SourcePresence.ETUDIANT);
        // Etudiant en difficulte avec son telephone : ajoute a la main par le formateur.
        marquerPresent(sessionId, CANDIDAT_3, SourcePresence.FORMATEUR);

        long exerciceId = deposerExercice(sessionId, AUTEUR, "EN_ATTENTE");

        Relecture relecture = relectureRepository.findByExercice_Id(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteurId()).isEqualTo(CANDIDAT_3);
    }

    @Test
    @DisplayName("le tirage est varie et ne designe jamais l'auteur (Q7, RG6)")
    void le_tirage_est_varie_et_ne_designe_jamais_l_auteur() throws Exception {
        Set<Long> relecteurs = new HashSet<>();

        for (int tirage = 0; tirage < 20; tirage++) {
            long sessionId = ouvrirSession();
            marquerPresent(sessionId, AUTEUR, SourcePresence.ETUDIANT);
            marquerPresent(sessionId, CANDIDAT_1, SourcePresence.ETUDIANT);
            marquerPresent(sessionId, CANDIDAT_2, SourcePresence.ETUDIANT);
            marquerPresent(sessionId, CANDIDAT_3, SourcePresence.ETUDIANT);

            long exerciceId = deposerExercice(sessionId, AUTEUR, "EN_ATTENTE");
            Relecture relecture = relectureRepository.findByExercice_Id(exerciceId).orElseThrow();

            assertThat(relecture.getRelecteurId()).isNotEqualTo(AUTEUR);
            relecteurs.add(relecture.getRelecteurId());
        }

        // Sur 20 tirages a trois candidats, obtenir 20 fois le meme relecteur a une
        // probabilite de 3^-19 : un echec signalerait un tirage non aleatoire.
        assertThat(relecteurs).hasSizeGreaterThanOrEqualTo(2);
    }

    // ------------------------------------------------------------------ outils

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance tirage", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("id").asLong();
    }

    private void marquerPresent(long sessionId, long etudiantId, SourcePresence source) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        Etudiant etudiant = etudiantRepository.findById(etudiantId).orElseThrow();
        presenceRepository.saveAndFlush(
                new Presence(session, etudiant, source, LocalDateTime.now(ZoneOffset.UTC)));
    }

    /** Depose un exercice et verifie le statut renvoye, en renvoyant l'identifiant cree. */
    private long deposerExercice(long sessionId, long etudiantId, String statutAttendu) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": %d, "etudiantId": %d, "lien": "%s" }
                                """.formatted(sessionId, etudiantId, LIEN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value(statutAttendu))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(corps);
        return json.get("id").asLong();
    }
}
