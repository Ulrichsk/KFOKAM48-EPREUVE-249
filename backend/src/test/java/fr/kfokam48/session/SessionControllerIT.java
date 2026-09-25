package fr.kfokam48.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP de {@code POST /api/sessions} (EF1, RG1, RG2).
 *
 * <p>Executes sur H2 en memoire avec les migrations Flyway reelles : aucun
 * PostgreSQL n'est requis.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("une session ouverte renvoie 201 avec un code valable 15 minutes (EF1, RG2)")
    void ouvre_une_session_avec_un_code_valable_quinze_minutes() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique — seance 4", "promotionId": 1 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(corps);
        String code = json.get("code").asText();
        Instant ouvertureAt = Instant.parse(json.get("ouvertureAt").asText());
        Instant expirationAt = Instant.parse(json.get("expirationAt").asText());

        // RG1 : format du code
        assertThat(code).hasSize(6).matches("^[" + GenerateurCodeSession.ALPHABET + "]+$");
        // RG2 : expiration = ouverture + 15 minutes, exactement
        assertThat(Duration.between(ouvertureAt, expirationAt))
                .isEqualTo(Session.DUREE_VALIDITE_CODE);
        // Le contrat expose bien les quatre champs attendus, et eux seuls
        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "code", "ouvertureAt", "expirationAt");
    }

    @Test
    @DisplayName("deux sessions ouvertes ont des codes differents (RG1)")
    void deux_sessions_ont_des_codes_differents() throws Exception {
        String premier = ouvrirEtLireCode("Seance 5");
        String second = ouvrirEtLireCode("Seance 6");
        assertThat(premier).isNotEqualTo(second);
    }

    @Test
    @DisplayName("un titre absent renvoie 400 CHAMP_MANQUANT (EF1)")
    void refuse_un_titre_absent() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "promotionId": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un titre blanc renvoie 400 CHAMP_MANQUANT (EF1)")
    void refuse_un_titre_blanc() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "   ", "promotionId": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("une promotion inconnue renvoie 404 PROMOTION_INCONNUE (EF1)")
    void refuse_une_promotion_inconnue() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance orpheline", "promotionId": 9999 }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("un corps illisible renvoie 400 au format impose (ENF4)")
    void refuse_un_corps_illisible() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ ceci n'est pas du json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALEUR_INVALIDE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private String ouvrirEtLireCode(String titre) throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "%s", "promotionId": 1 }
                                """.formatted(titre)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps).get("code").asText();
    }
}
