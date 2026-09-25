package fr.kfokam48.commun.erreur;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie le filet de securite de ENF4 : une exception imprevue ne doit jamais
 * renvoyer de trace, seulement le message generique au format impose.
 *
 * <p>Le controleur fautif vit dans la classe de test : il n'existe pas dans le
 * code de production, ce qui garantit qu'aucune route de l'application ne leve
 * volontairement une erreur interne.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GestionnaireErreursIT.ControleurFautif.class)
class GestionnaireErreursIT {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class ControleurFautif {

        @GetMapping("/api/test/erreur-interne")
        String provoquerUneErreurInterne() {
            throw new IllegalStateException("panne simulee : ne doit jamais atteindre le client");
        }
    }

    @Test
    @DisplayName("une exception imprevue renvoie 500 ERREUR_INTERNE sans aucune trace (ENF4)")
    void exception_imprevue_ne_fuite_pas_de_trace() throws Exception {
        mockMvc.perform(get("/api/test/erreur-interne"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERREUR_INTERNE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.message").value(
                        "Une erreur interne est survenue. Merci de reessayer plus tard."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist());
    }
}
