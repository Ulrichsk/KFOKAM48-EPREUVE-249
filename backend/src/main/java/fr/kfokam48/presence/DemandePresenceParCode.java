package fr.kfokam48.presence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences} (contrat impose) : {@code { code, etudiantId }}.
 *
 * <p>Le code est dicte a l'oral puis recopie a la main (Q1, Q2). Les espaces et la
 * casse sont donc normalises par le service avant comparaison, afin qu'une saisie
 * approximative ne produise pas une erreur incomprehensible (hypothese H9).</p>
 */
public record DemandePresenceParCode(

        @NotBlank(message = "Le code de presence est obligatoire.")
        String code,

        @NotNull(message = "L'etudiant est obligatoire.")
        Long etudiantId
) {
}
