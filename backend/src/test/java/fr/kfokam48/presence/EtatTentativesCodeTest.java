package fr.kfokam48.presence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire de RG5 (Q4) : cinq erreurs bloquent deux minutes.
 *
 * <p>Test unitaire pur — ni base, ni contexte Spring, ni attente reelle : l'objet
 * de valeur fixe l'instant qui l'interesse. C'est la raison d'etre de
 * {@link EtatTentativesCode}, qui ne connait ni le temps systeme ni la persistance.</p>
 */
class EtatTentativesCodeTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 25, 8, 0);

    @Test
    @DisplayName("quatre erreurs ne bloquent pas (Q4)")
    void quatre_erreurs_ne_bloquent_pas() {
        EtatTentativesCode etat = EtatTentativesCode.initial();
        for (int echec = 0; echec < 4; echec++) {
            etat = etat.apresEchec(T0);
        }

        assertThat(etat.nbEchecs()).isEqualTo(4);
        assertThat(etat.bloqueJusquA()).isNull();
        assertThat(etat.bloqueA(T0)).isFalse();
    }

    @Test
    @DisplayName("la cinquieme erreur bloque pendant deux minutes exactement (Q4)")
    void la_cinquieme_erreur_bloque_deux_minutes() {
        EtatTentativesCode etat = EtatTentativesCode.initial();
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            etat = etat.apresEchec(T0);
        }

        assertThat(etat.bloqueJusquA()).isEqualTo(T0.plusMinutes(2));
        assertThat(EtatTentativesCode.DUREE_BLOCAGE).isEqualTo(java.time.Duration.ofMinutes(2));
        // L'echec qui declenche le blocage n'est plus compte : l'etudiant repartira
        // de zero apres le delai, sinon il serait rebloque des sa premiere erreur.
        assertThat(etat.nbEchecs()).isZero();
    }

    @Test
    @DisplayName("le blocage court jusqu'a l'instant limite exclu (Q4)")
    void le_blocage_court_jusqu_a_l_instant_limite() {
        EtatTentativesCode etat = EtatTentativesCode.initial();
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            etat = etat.apresEchec(T0);
        }

        assertThat(etat.bloqueA(T0)).isTrue();
        assertThat(etat.bloqueA(T0.plusSeconds(119))).isTrue();
        // Deux minutes ecoulees : les tentatives sont de nouveau possibles.
        assertThat(etat.bloqueA(T0.plusMinutes(2))).isFalse();
        assertThat(etat.bloqueA(T0.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("apres le blocage, il faut a nouveau cinq erreurs pour rebloquer (Q4)")
    void apres_le_blocage_il_faut_a_nouveau_cinq_erreurs() {
        EtatTentativesCode etat = EtatTentativesCode.initial();
        for (int echec = 0; echec < EtatTentativesCode.SEUIL_ECHECS; echec++) {
            etat = etat.apresEchec(T0);
        }

        LocalDateTime apresLeDelai = T0.plusMinutes(3);
        for (int echec = 0; echec < 4; echec++) {
            etat = etat.apresEchec(apresLeDelai);
        }

        assertThat(etat.bloqueA(apresLeDelai)).isFalse();
        assertThat(etat.nbEchecs()).isEqualTo(4);
    }

    @Test
    @DisplayName("une reussite efface le compteur et tout blocage (Q4)")
    void une_reussite_remet_tout_a_zero() {
        EtatTentativesCode etat = EtatTentativesCode.initial();
        for (int echec = 0; echec < 3; echec++) {
            etat = etat.apresEchec(T0);
        }

        EtatTentativesCode apresReussite = etat.apresReussite();

        assertThat(apresReussite).isEqualTo(EtatTentativesCode.initial());
        assertThat(apresReussite.nbEchecs()).isZero();
        assertThat(apresReussite.bloqueA(T0)).isFalse();
    }
}
