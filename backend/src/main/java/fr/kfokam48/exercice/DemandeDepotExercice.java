package fr.kfokam48.exercice;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/exercices} (contrat impose) :
 * {@code { sessionId, etudiantId, lien }}.
 *
 * <p>Le {@code lien} n'est volontairement pas annote {@code @NotBlank} : le contrat
 * attend {@code 400 LIEN_INVALIDE} pour un lien vide, relatif ou mal forme, et non
 * {@code CHAMP_MANQUANT}. C'est donc le service qui le valide
 * ({@link ValidateurLien}), avec un seul code d'erreur pour tous les cas.</p>
 */
public record DemandeDepotExercice(

        @NotNull(message = "La session est obligatoire.")
        Long sessionId,

        @NotNull(message = "L'etudiant est obligatoire.")
        Long etudiantId,

        String lien
) {
}
