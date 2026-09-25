package fr.kfokam48.relecture;

/**
 * Origine de la designation du relecteur.
 *
 * <p>Conservee pour rendre le dispositif auditable : un relecteur tire au hasard
 * ({@code SYSTEME}, Q7) et un relecteur designe par le formateur apres un tirage
 * infructueux ({@code FORMATEUR}, RG10, decision 7.4 du cahier des charges) n'ont pas
 * la meme valeur, et le formateur doit pouvoir le verifier.</p>
 */
public enum AssignePar {

    SYSTEME,

    FORMATEUR
}
