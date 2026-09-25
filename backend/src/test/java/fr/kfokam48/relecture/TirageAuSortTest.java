package fr.kfokam48.relecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire du tirage au sort (Q7, RG7-RG8).
 *
 * <p>Test unitaire pur : la classe testee ne connait ni les etudiants, ni la base, ni le
 * contexte Spring. Les probabilites sont choisies pour rendre un echec accidentel
 * impossible en pratique (voir les commentaires de chaque test).</p>
 */
class TirageAuSortTest {

    private final TirageAuSort tirage = new TirageAuSort();

    @Test
    @DisplayName("une liste vide ne designe personne (RG10 : c'est au service de decider)")
    void une_liste_vide_ne_designe_personne() {
        assertThat(tirage.choisirParmi(List.of())).isEmpty();
    }

    @Test
    @DisplayName("une liste absente ne designe personne")
    void une_liste_absente_ne_designe_personne() {
        assertThat(tirage.choisirParmi(null)).isEmpty();
    }

    @Test
    @DisplayName("un candidat unique est toujours designe (Q6 : un seul relecteur)")
    void un_candidat_unique_est_toujours_designe() {
        for (int essai = 0; essai < 50; essai++) {
            assertThat(tirage.choisirParmi(List.of("unique"))).contains("unique");
        }
    }

    @Test
    @DisplayName("le tirage ne sort jamais de la liste des candidats (RG8)")
    void le_tirage_ne_sort_jamais_de_la_liste() {
        List<String> candidats = List.of("Amina", "Bertrand", "Chantal");

        for (int essai = 0; essai < 500; essai++) {
            assertThat(tirage.choisirParmi(candidats)).isPresent().get().isIn(candidats);
        }
    }

    @Test
    @DisplayName("le tirage est varie : il ne designe pas toujours le meme candidat (Q7)")
    void le_tirage_est_varie() {
        List<String> candidats = List.of("Amina", "Bertrand", "Chantal", "Didier");
        Set<String> designes = new HashSet<>();

        for (int essai = 0; essai < 300; essai++) {
            tirage.choisirParmi(candidats).ifPresent(designes::add);
        }

        // Sur 300 tirages a quatre candidats, n'obtenir qu'un ou deux designes
        // distincts releverait d'un generateur casse, pas de la malchance.
        assertThat(designes).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("le tirage accepte des listes modifiables comme non modifiables")
    void le_tirage_accepte_toute_liste() {
        List<String> modifiable = new ArrayList<>(List.of("Amina", "Bertrand"));

        assertThat(tirage.choisirParmi(modifiable)).isPresent();
    }
}
