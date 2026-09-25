package fr.kfokam48.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire de RG1 : format et qualite du code de presence.
 *
 * <p>Test unitaire pur — aucun contexte Spring, aucune base, aucune horloge : il
 * s'execute en quelques millisecondes sur n'importe quel poste (ENF9).</p>
 */
class GenerateurCodeSessionTest {

    private final GenerateurCodeSession generateur = new GenerateurCodeSession();

    @Test
    @DisplayName("le code fait exactement 6 caracteres (RG1)")
    void le_code_a_la_longueur_imposee() {
        for (int i = 0; i < 500; i++) {
            assertThat(generateur.generer()).hasSize(GenerateurCodeSession.LONGUEUR);
        }
    }

    @Test
    @DisplayName("le code n'utilise que l'alphabet restreint, donc aucun caractere ambigu (RG1)")
    void le_code_evite_les_caracteres_ambigus() {
        for (int i = 0; i < 500; i++) {
            String code = generateur.generer();
            assertThat(code).matches("^[" + GenerateurCodeSession.ALPHABET + "]+$");
            // Les caracteres confondus a l'oral (Q2) n'apparaissent jamais.
            assertThat(code).doesNotContain("0", "O", "1", "I", "L");
        }
    }

    @Test
    @DisplayName("deux tirages successifs produisent des codes differents (RG1)")
    void les_codes_sont_varies() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generateur.generer());
        }
        // 31^6 combinaisons : sur 1000 tirages, une collision generalisee
        // signifierait un generateur casse ou un alphabet tronque.
        assertThat(codes).hasSizeGreaterThan(990);
    }

    @Test
    @DisplayName("le code est dicte a l'oral : il ne contient jamais d'espace ni de minuscule (RG1)")
    void le_code_est_facile_a_dicter() {
        String code = generateur.generer();
        assertThat(code).isEqualTo(code.toUpperCase());
        assertThat(code).doesNotContain(" ", "-", "_");
    }
}
