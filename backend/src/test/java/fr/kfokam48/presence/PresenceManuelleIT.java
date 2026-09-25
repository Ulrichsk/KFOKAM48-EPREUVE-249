package fr.kfokam48.presence;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/sessions/{id}/presences} (EF4, Q14,
 * RG4, RG17, RG19, RG21) — issue 14.
 *
 * <p>Etudiants utilises : 3 (regularise, promotion 1 — jamais present dans les autres
 * classes) et 14 (promotion 2, pour le controle RG19). Les identifiants 1 a 12 de la
 * promotion 1 sont deja charges d'activite ou de compteurs de blocage par les autres
 * classes, le contexte Spring et la base H2 etant partages.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceManuelleIT {

    private static final long PROMOTION_1 = 1L;
    private static final long ETUDIANT_REGULARISE = 3L;
    private static final long ETUDIANT_PROMOTION_2 = 14L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    // ------------------------------------------------------------------ cas nominal

    @Test
    @DisplayName("le formateur ajoute une presence : 201 avec source FORMATEUR (Q14, RG17)")
    void ajoute_une_presence_manuelle() throws Exception {
        long sessionId = ouvrirSession();

        String corps = ajouter(sessionId, ETUDIANT_REGULARISE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.etudiantId").value(ETUDIANT_REGULARISE))
                .andExpect(jsonPath("$.source").value("FORMATEUR"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Champs du contrat, et strictement rien d'autre.
        JsonNode presence = objectMapper.readTree(corps);
        assertThat(presence.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "sessionId", "etudiantId", "source");

        assertThat(presenceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("l'etudiant peut encore saisir le code : 409 DEJA_PRESENT, la presence n'est pas dupliquee (RG4)")
    void refuse_le_doublon_etudiant_apres_ajout_manuel() throws Exception {
        long sessionId = ouvrirSession();
        String code = codeDe(sessionId);

        ajouter(sessionId, ETUDIANT_REGULARISE).andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "%s", "etudiantId": %d }
                                """.formatted(code, ETUDIANT_REGULARISE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // RG4 : toujours une seule ligne, la source d'origine est conservee.
        assertThat(presenceRepository.countBySession_Id(sessionId)).isEqualTo(1L);
        assertThat(presenceRepository.findBySession_IdAndEtudiant_Id(sessionId, ETUDIANT_REGULARISE)
                .orElseThrow().getSource()).isEqualTo(SourcePresence.FORMATEUR);
    }

    @Test
    @DisplayName("deux ajouts manuels du meme etudiant : 409 DEJA_PRESENT (RG4)")
    void refuse_un_second_ajout_manuel() throws Exception {
        long sessionId = ouvrirSession();

        ajouter(sessionId, ETUDIANT_REGULARISE).andExpect(status().isCreated());

        ajouter(sessionId, ETUDIANT_REGULARISE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    // ------------------------------------------------------------------ garde-fous

    @Test
    @DisplayName("apres clôture : ajout manuel refuse 409 SESSION_CLOTUREE (RG21)")
    void refuse_l_ajout_apres_cloture() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId))
                .andExpect(status().isOk());

        ajouter(sessionId, ETUDIANT_REGULARISE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(presenceRepository.countBySession_Id(sessionId)).isZero();
    }

    @Test
    @DisplayName("un etudiant d'une autre promotion est refuse : 403 ACCES_REFUSE (RG19)")
    void refuse_un_etudiant_d_une_autre_promotion() throws Exception {
        long sessionId = ouvrirSession();

        ajouter(sessionId, ETUDIANT_PROMOTION_2)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(presenceRepository.countBySession_Id(sessionId)).isZero();
    }

    @Test
    @DisplayName("etudiant inconnu : 404 ETUDIANT_INCONNU ; session inconnue : 404 SESSION_INTROUVABLE")
    void refuse_les_ressources_inconnues() throws Exception {
        long sessionId = ouvrirSession();

        ajouter(sessionId, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));

        ajouter(999_999L, ETUDIANT_REGULARISE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INTROUVABLE"));
    }

    @Test
    @DisplayName("corps sans etudiantId : 400 CHAMP_MANQUANT (contrat)")
    void refuse_un_corps_incomplet() throws Exception {
        long sessionId = ouvrirSession();

        mockMvc.perform(post("/api/sessions/{id}/presences", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));

        assertThat(presenceRepository.countBySession_Id(sessionId)).isZero();
    }

    // ------------------------------------------------------------------ outils

    private long ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance presence manuelle EF4", "promotionId": %d }
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

    private ResultActions ajouter(long sessionId, long etudiantId) throws Exception {
        return mockMvc.perform(post("/api/sessions/{id}/presences", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "etudiantId": %d }
                        """.formatted(etudiantId)));
    }
}
