package fr.kfokam48.commun.erreur;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Format d'erreur impose par le sujet, identique pour toutes les erreurs :
 *
 * <pre>{@code { "code": "CODE_EXPIRE", "message": "Le code de presence a expire." } }</pre>
 *
 * <p>Le record ne porte volontairement que deux champs : aucune trace, aucun
 * detail technique, aucun horodatage ne peut donc fuiter par ce chemin (ENF4).
 * {@code @JsonInclude(NON_NULL)} garantit qu'aucun champ vide n'apparait.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReponseErreur(String code, String message) {

    public static ReponseErreur de(CodeErreur code, String message) {
        return new ReponseErreur(code.name(), message);
    }
}
