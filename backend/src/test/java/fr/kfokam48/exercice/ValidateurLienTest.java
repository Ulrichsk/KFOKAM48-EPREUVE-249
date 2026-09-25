package fr.kfokam48.exercice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire de RG22 : forme acceptee du lien d'exercice.
 *
 * <p>Test unitaire pur : aucun contexte Spring, aucune base. La regle est verifiee sur
 * les cas qui posent vraiment probleme en production — scheme detourne, lien relatif,
 * longueur superieure a la colonne de la base.</p>
 */
class ValidateurLienTest {

    private final ValidateurLien validateur = new ValidateurLien();

    @ParameterizedTest
    @DisplayName("les URL absolues http et https sont acceptees (RG22)")
    @ValueSource(strings = {
            "http://example.invalid/rendus/2.pdf",
            "https://example.invalid/rendus/2.pdf",
            "https://example.invalid:8443/depots/2026/exercice-4.pdf",
            "https://example.invalid/rendus/2.pdf?version=2&auteur=2",
            "https://example.invalid",
            "HTTPS://EXAMPLE.INVALID/RENDUS/2.PDF"
    })
    void accepte_les_url_absolues_http_et_https(String lien) {
        assertThat(validateur.estValide(lien)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("un lien relatif, vide ou absent est refuse (RG22)")
    @ValueSource(strings = {"", "   ", "/rendus/2.pdf", "rendus/2.pdf", "www.example.invalid/x.pdf", "2.pdf"})
    void refuse_un_lien_relatif_ou_vide(String lien) {
        assertThat(validateur.estValide(lien)).isFalse();
    }

    @Test
    @DisplayName("un lien absent est refuse (RG22)")
    void refuse_un_lien_absent() {
        assertThat(validateur.estValide(null)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("les schemes non web sont refuses : ils seraient cliquables chez le relecteur (RG22)")
    @ValueSource(strings = {
            "ftp://example.invalid/2.pdf",
            "file:///tmp/2.pdf",
            "javascript:alert(1)",
            "data:text/html;base64,PHNjcmlwdD4=",
            "mailto:etudiant@example.invalid",
            "sftp://example.invalid/x"
    })
    void refuse_les_schemes_non_web(String lien) {
        assertThat(validateur.estValide(lien)).isFalse();
    }

    @Test
    @DisplayName("une URL sans hote est refusee (RG22)")
    void refuse_une_url_sans_hote() {
        assertThat(validateur.estValide("http://")).isFalse();
        assertThat(validateur.estValide("https:///rendus/2.pdf")).isFalse();
    }

    @Test
    @DisplayName("un lien illisible est refuse (RG22)")
    void refuse_un_lien_illisible() {
        assertThat(validateur.estValide("https://exa mple.invalid/2.pdf")).isFalse();
        assertThat(validateur.estValide("https://example.invalid/2[1].pdf")).isFalse();
    }

    @Test
    @DisplayName("la limite de longueur suit la colonne de la base : 500 accepte, 501 refuse (RG22)")
    void respecte_la_longueur_de_la_colonne() {
        String prefixe = "https://example.invalid/";

        String juste = prefixe + "a".repeat(ValidateurLien.LONGUEUR_MAX - prefixe.length());
        String trop = prefixe + "a".repeat(ValidateurLien.LONGUEUR_MAX - prefixe.length() + 1);

        assertThat(juste).hasSize(ValidateurLien.LONGUEUR_MAX);
        assertThat(validateur.estValide(juste)).isTrue();
        assertThat(trop).hasSize(ValidateurLien.LONGUEUR_MAX + 1);
        assertThat(validateur.estValide(trop)).isFalse();
    }
}
