package fr.kfokam48.exercice;

/**
 * Corps de {@code PUT /api/exercices/{id}} (schema {@code DemandeRemplacementLien} du
 * contrat) : {@code { lien }}.
 *
 * <p>L'identite de l'appelant ne transite pas par le corps : c'est le header
 * {@code X-Etudiant-Id} (identite declarative, Q1), seul l'auteur etant admis (Q13).</p>
 *
 * <p>Le {@code lien} n'est volontairement pas annote {@code @NotBlank} (issue 15) :
 * le contrat ne documente qu'un seul 400 pour cette operation — {@code LIEN_INVALIDE}
 * (reference {@code LienInvalide}) — et un lien vide en fait partie, puisqu'il n'est
 * pas une URL absolue. La validation appartient donc au service ({@link ValidateurLien}),
 * exactement comme pour le depot ({@link DemandeDepotExercice}) : un seul code d'erreur
 * pour tous les cas (vide, relatif, mal forme), conforme a la decision de conception
 * posee des l'issue 05. Avant correctif, l'annotation court-circuitait le service et
 * produisait un {@code CHAMP_MANQUANT} jamais promis par le contrat.</p>
 */
public record DemandeRemplacementLien(

        String lien
) {
}
