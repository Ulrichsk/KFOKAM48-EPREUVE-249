package fr.kfokam48.presence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration HTTP du blocage des tentatives de code (EF13, RG5, Q4).
 *
 * <p>Ces tests verifient ce que le test unitaire de {@link EtatTentativesCode} ne peut
 * pas montrer : que l'increment d'echecs <b>survit</b> a l'echec de la tentative. C'est
 * le point delicat de cette issue — l'exception metier annule la transaction, donc le
 * compteur est mis a jour dans une transaction separee.</p>
 *
 * <p>Un etudiant different est utilise par test : le compteur est persistant et la base
 * H2 est partagee par tout le contexte de test, deux tests ne doivent donc pas
 * comptabiliser leurs echecs sur le meme etudiant. Etudiants 1 et 13 deja utilises
 * ailleurs, on prend ici les etudiants 5 a 10 de la promotion 1.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BlocageTentativesCodeIT {

    private static final long PROMOTION_1 = 1L;

    /** Code syntaxiquement plausible mais attribue a aucune session. */
    private static final String CODE_IMPOSSIBLE = "QQQQQ2";

    private static final long ETUDIANT_BLOCAGE = 5L;
    private static final long ETUDIANT_BON_CODE = 6L;
    private static final long ETUDIANT_DELAI = 7L;
    private static final long ETUDIANT_REUSSITE = 8L;
    private static final long ETUDIANT_BLOQUE = 9L;
    private static final long ETUDIANT_TEMOIN = 10L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TentativeCodeRepository tentativeCodeRepository;

    @Test
    @DisplayName("cinq erreurs bloquent la sixieme tentative deux minutes (Q4)")
    void cinq_erreurs_bloquent_la_sixieme_tentative() throws Exception {
        for (int echec = 1; echec <= EtatTentativesCode.SEUIL_ECHECS; echec++) {
            // L'erreur d'origine est renvoyee, y compris pour la cinquieme : c'est la
            // tentative suivante qui est refusee.
            tentative(CODE_IMPOSSIBLE, ETUDIANT_BLOCAGE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }

        tentative(CODE_IMPOSSIBLE, ETUDIANT_BLOCAGE)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Le compteur a bien survecu aux cinq exceptions metier.
        TentativeCode compteur = tentativeCodeRepository.findByEtudiantId(ETUDIANT_BLOCAGE).orElseThrow();
        assertThat(compteur.getNbEchecs()).isZero();
        assertThat(compteur.getBloqueJusquA()).isNotNull();
    }

    @Test
    @DisplayName("un etudiant bloque ne peut pas se debloquer avec le bon code (Q4)")
    void le_bon_code_ne_contourne_pas_le_blocage() throws Exception {
        String bonCode = ouvrirSession().get("code").asText();
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            tentative(CODE_IMPOSSIBLE, ETUDIANT_BON_CODE).andExpect(status().isBadRequest());
        }

        tentative(bonCode, ETUDIANT_BON_CODE)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"));
    }

    @Test
    @DisplayName("le delai ecoule, la tentative est de nouveau traitee (Q4)")
    void apres_le_delai_la_tentative_est_traitee_normalement() throws Exception {
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            tentative(CODE_IMPOSSIBLE, ETUDIANT_DELAI).andExpect(status().isBadRequest());
        }
        tentative(CODE_IMPOSSIBLE, ETUDIANT_DELAI).andExpect(status().isTooManyRequests());

        // On deplace l'echeance du blocage dans le passe plutot que d'attendre deux
        // minutes : l'arithmetique du delai est deja couverte par le test unitaire.
        faireExpirerLeBlocage(ETUDIANT_DELAI);

        tentative(CODE_IMPOSSIBLE, ETUDIANT_DELAI)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
    }

    @Test
    @DisplayName("une reussite remet le compteur a zero (Q4)")
    void une_reussite_remet_le_compteur_a_zero() throws Exception {
        String code = ouvrirSession().get("code").asText();
        for (int echec = 0; echec < 4; echec++) {
            tentative(CODE_IMPOSSIBLE, ETUDIANT_REUSSITE).andExpect(status().isBadRequest());
        }

        tentative(code, ETUDIANT_REUSSITE).andExpect(status().isCreated());

        // Quatre nouvelles erreurs : toujours aucun blocage, le compteur est reparti de zero.
        for (int echec = 0; echec < 4; echec++) {
            tentative(CODE_IMPOSSIBLE, ETUDIANT_REUSSITE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }
    }

    @Test
    @DisplayName("le blocage ne concerne que l'etudiant fautif (H7)")
    void le_blocage_ne_concerne_que_l_etudiant_fautif() throws Exception {
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            tentative(CODE_IMPOSSIBLE, ETUDIANT_BLOQUE).andExpect(status().isBadRequest());
        }
        tentative(CODE_IMPOSSIBLE, ETUDIANT_BLOQUE).andExpect(status().isTooManyRequests());

        String code = ouvrirSession().get("code").asText();
        tentative(code, ETUDIANT_TEMOIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    // ------------------------------------------------------------------ outils

    private ResultActions tentative(String code, long etudiantId) throws Exception {
        return mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "%s", "etudiantId": %d }
                        """.formatted(code, etudiantId)));
    }

    private JsonNode ouvrirSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Seance blocage", "promotionId": %d }
                                """.formatted(PROMOTION_1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(corps);
    }

    private void faireExpirerLeBlocage(long etudiantId) {
        TentativeCode compteur = tentativeCodeRepository.findByEtudiantId(etudiantId).orElseThrow();
        LocalDateTime maintenant = LocalDateTime.now(ZoneOffset.UTC);
        compteur.appliquer(new EtatTentativesCode(0, maintenant.minusMinutes(3)), maintenant);
        tentativeCodeRepository.saveAndFlush(compteur);
    }
}
