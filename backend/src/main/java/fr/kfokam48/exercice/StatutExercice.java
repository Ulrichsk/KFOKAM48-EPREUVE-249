package fr.kfokam48.exercice;

/**
 * Cycle de vie d'un exercice (voir {@code docs/diagrammes/D4.md}).
 *
 * <p>Seules les valeurs {@code EN_ATTENTE}, {@code SANS_RELECTEUR} et {@code RELU} sont
 * exposees par l'API : ce sont celles du contrat. {@code DEPOSE} existe en base sans etre
 * renvoyee au client — c'est l'etat transitoire de la transaction de depot, avant que le
 * tirage au sort du relecteur n'ait eu lieu (issue 07).</p>
 */
public enum StatutExercice {

    /** Etat transitoire, interne a la transaction de depot : jamais renvoye au client. */
    DEPOSE,

    /** Un relecteur est designe et n'a pas encore rendu sa note (Q11). */
    EN_ATTENTE,

    /** Aucun etudiant eligible au tirage : le formateur doit designer un relecteur (RG10). */
    SANS_RELECTEUR,

    /** La note a ete envoyee : etat terminal (Q15, RG12). */
    RELU
}
