package fr.kfokam48.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * Ressource absente : repond toujours 404 avec le code du contrat
 * ({@code PROMOTION_INCONNUE}, {@code SESSION_INTROUVABLE}, ...).
 */
public class RessourceIntrouvableException extends ErreurMetier {

    public RessourceIntrouvableException(CodeErreur code, String message) {
        super(code, HttpStatus.NOT_FOUND, message);
    }
}
