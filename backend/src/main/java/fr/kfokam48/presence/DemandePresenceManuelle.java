package fr.kfokam48.presence;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/sessions/{id}/presences} (schema
 * {@code DemandePresenceManuelle} du contrat) : {@code { etudiantId }}.
 *
 * <p>Le formateur designe l'etudiant par la meme liste de Q1 : aucune saisie libre,
 * aucun mot de passe. La session visee est dans le chemin, sa source de verite est
 * l'identifiant du formateur, pas un code dicte.</p>
 */
public record DemandePresenceManuelle(

        @NotNull(message = "L'etudiant est obligatoire.")
        Long etudiantId
) {
}
