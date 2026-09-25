package fr.kfokam48.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/sessions} (contrat impose).
 *
 * <p>La validation est declarative : un champ absent, vide ou blanc produit une
 * {@code MethodArgumentNotValidException}, que le {@code GestionnaireErreurs}
 * traduit en {@code 400 CHAMP_MANQUANT} — le seul code 400 prevu par le contrat
 * pour cette operation.</p>
 */
public record DemandeOuvertureSession(

        @NotBlank(message = "Le titre de la session est obligatoire.")
        @Size(max = 160, message = "Le titre de la session est trop long.")
        String titre,

        @NotNull(message = "La promotion est obligatoire.")
        Long promotionId
) {
}
