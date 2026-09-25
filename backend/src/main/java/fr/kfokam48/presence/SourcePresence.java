package fr.kfokam48.presence;

/**
 * Origine de la saisie d'une presence (Q14, RG17).
 *
 * <p>L'information est conservee et affichee : un ajout manuel du formateur doit
 * « se voir » dans le tableau. Elle ne filtre en revanche jamais l'eligibilite au
 * tirage au sort du relecteur (decision 7.2 du cahier des charges).</p>
 */
public enum SourcePresence {

    /** Presence saisie par l'etudiant lui-meme avec le code de la session. */
    ETUDIANT,

    /** Presence ajoutee a la main par le formateur (souci de telephone, Q14). */
    FORMATEUR
}
