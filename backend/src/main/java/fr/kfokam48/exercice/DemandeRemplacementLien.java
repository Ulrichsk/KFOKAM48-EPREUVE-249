package fr.kfokam48.exercice;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de {@code PUT /api/exercices/{id}} (schema {@code DemandeRemplacementLien} du
 * contrat) : {@code { lien }}.
 *
 * <p>L'identite de l'appelant ne transite pas par le corps : c'est le header
 * {@code X-Etudiant-Id} (identite declarative, Q1), seul l'auteur etant admis (Q13).</p>
 */
public record DemandeRemplacementLien(

        @NotBlank(message = "Le lien est obligatoire.")
        String lien
) {
}
