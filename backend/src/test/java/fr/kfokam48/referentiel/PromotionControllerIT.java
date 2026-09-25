package fr.kfokam48.referentiel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP du referentiel (ENF9).
 *
 * <p>Ils tournent sur la base H2 en memoire du profil de test : aucun
 * PostgreSQL ni aucune base locale n'est necessaire, et les migrations Flyway
 * reelles sont jouees a chaque execution.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PromotionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/promotions/{id}/etudiants renvoie les etudiants triee par nom (EF15)")
    void liste_les_etudiants_d_une_promotion_triee_par_nom() throws Exception {
        mockMvc.perform(get("/api/promotions/1/etudiants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].nom").value("Diallo"))
                .andExpect(jsonPath("$[0].prenom").value("Amina"))
                .andExpect(jsonPath("$[0].promotionId").value(1))
                .andExpect(jsonPath("$[11].nom").value("Zogo"));
    }

    @Test
    @DisplayName("une promotion inconnue renvoie 404 au format d'erreur impose (EF15)")
    void refuse_une_promotion_inconnue() throws Exception {
        mockMvc.perform(get("/api/promotions/9999/etudiants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                // Aucune trace, aucun detail technique dans la reponse (ENF4)
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("une route inconnue respecte elle aussi le format d'erreur impose (ENF4)")
    void route_inconnue_respecte_le_format_impose() throws Exception {
        mockMvc.perform(get("/api/route-inexistante"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("un identifiant de mauvais type renvoie 400 VALEUR_INVALIDE (ENF4)")
    void identifiant_invalide_renvoie_400() throws Exception {
        mockMvc.perform(get("/api/promotions/abc/etudiants"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALEUR_INVALIDE"));
    }
}
