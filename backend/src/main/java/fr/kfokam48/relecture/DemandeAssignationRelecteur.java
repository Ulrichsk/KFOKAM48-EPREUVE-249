package fr.kfokam48.relecture;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/exercices/{id}/relecteur} (contrat) : {@code { relecteurId }}.
 *
 * <p>Seul le relecteur propose est transmis : l'exercice est designe par le chemin, et
 * l'auteur de l'operation n'a pas a l'etre puisqu'aucune authentification n'existe
 * (Q1, H1) — le formateur est le seul appelant de cette operation.</p>
 *
 * <p>Le champ est annote {@code @NotNull} : son absence est une demande incomplete, donc
 * {@code 400 CHAMP_MANQUANT}. C'est different du lien du depot, ou le contrat attend
 * {@code LIEN_INVALIDE} : ici, aucun code d'erreur plus precis n'existe pour un corps
 * vide, et le contrat reference explicitement {@code CHAMP_MANQUANT}.</p>
 */
public record DemandeAssignationRelecteur(

        @NotNull(message = "Le relecteur est obligatoire.")
        Long relecteurId
) {
}
