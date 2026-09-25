package fr.kfokam48.presence;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Politique de blocage des tentatives de code (RG5, Q4) : objet de valeur pur.
 *
 * <p>Aucune dependance technique — ni base, ni horloge systeme, ni contexte Spring —
 * ce qui rend la regle entierement testable : les tests fixent l'instant qui les
 * interesse au lieu d'attendre reellement deux minutes.</p>
 *
 * <p>Q4 : « Au bout de cinq erreurs, bloquez-le deux minutes, sinon ils vont deviner
 * les codes entre eux. » Trois consequences sont codees ici :</p>
 * <ul>
 *   <li>le cinquieme echec declenche le blocage, mais c'est la tentative <b>suivante</b>
 *       qui recoit la reponse de blocage ;</li>
 *   <li>le compteur repart de zero au moment du blocage, sinon l'etudiant serait
 *       rebloque des sa premiere erreur apres le delai ;</li>
 *   <li>une reussite efface tout, y compris un blocage expire (Q4 ne parle que
 *       d'erreurs consecutives).</li>
 * </ul>
 *
 * @param nbEchecs      echecs consecutifs depuis la derniere reussite
 * @param bloqueJusquA  fin du blocage en cours, {@code null} si aucun blocage
 */
public record EtatTentativesCode(int nbEchecs, LocalDateTime bloqueJusquA) {

    /** Nombre d'echecs consecutifs qui declenche le blocage (Q4). */
    public static final int SEUIL_ECHECS = 5;

    /** Duree du blocage imposee par Q4. */
    public static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

    public static EtatTentativesCode initial() {
        return new EtatTentativesCode(0, null);
    }

    /**
     * Vrai si l'etudiant est encore bloque a cet instant.
     *
     * <p>Le blocage court jusqu'a l'instant limite exclu : une fois les deux minutes
     * ecoulees, la tentative est de nouveau traitee normalement.</p>
     */
    public boolean bloqueA(LocalDateTime maintenant) {
        return bloqueJusquA != null && maintenant.isBefore(bloqueJusquA);
    }

    /** Etat apres une tentative infructueuse (code inconnu ou expire, Q4). */
    public EtatTentativesCode apresEchec(LocalDateTime maintenant) {
        int echecs = nbEchecs + 1;
        if (echecs >= SEUIL_ECHECS) {
            return new EtatTentativesCode(0, maintenant.plus(DUREE_BLOCAGE));
        }
        return new EtatTentativesCode(echecs, null);
    }

    /** Etat apres une tentative reussie : plus aucun echec, plus aucun blocage. */
    public EtatTentativesCode apresReussite() {
        return initial();
    }
}
