package fr.kfokam48.commun.erreur;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestion centralisee des erreurs (ENF4).
 *
 * <p>Un seul point de sortie pour toutes les erreurs de l'API : chaque reponse
 * respecte strictement {@code { "code": "...", "message": "..." }}. Aucune trace,
 * aucun nom de classe interne, aucun message SQL n'est renvoye au client. La
 * trace complete est journalisee cote serveur uniquement, ce qui la rend
 * disponible pour le diagnostic sans jamais fuiter dans la reponse.</p>
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    /** Message generique : jamais de detail technique dans la reponse (ENF4). */
    private static final String MESSAGE_INTERNE =
            "Une erreur interne est survenue. Merci de reessayer plus tard.";

    /** Regles de gestion violees : le code et le statut viennent de l'exception. */
    @ExceptionHandler(ErreurMetier.class)
    public ResponseEntity<ReponseErreur> gererErreurMetier(ErreurMetier erreur) {
        return ResponseEntity.status(erreur.getStatut())
                .body(ReponseErreur.de(erreur.getCode(), erreur.getMessage()));
    }

    /** Corps de requete invalide au regard des annotations de validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReponseErreur> gererValidationDuCorps(MethodArgumentNotValidException erreur) {
        String champ = erreur.getBindingResult().getFieldErrors().stream()
                .map(champEnErreur -> champEnErreur.getField())
                .findFirst()
                .orElse(null);
        String message = champ == null
                ? "Un champ obligatoire est manquant."
                : "Le champ « " + champ + " » est obligatoire ou invalide.";
        return badRequest(CodeErreur.CHAMP_MANQUANT, message);
    }

    /** Parametre de requete obligatoire absent ({@code ?promotionId=}). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ReponseErreur> gererParametreManquant(MissingServletRequestParameterException erreur) {
        return badRequest(CodeErreur.CHAMP_MANQUANT,
                "Le parametre « " + erreur.getParameterName() + " » est obligatoire.");
    }

    /** En-tete obligatoire absent (identite declarative, cf. trou T10). */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ReponseErreur> gererEnTeteManquant(MissingRequestHeaderException erreur) {
        return badRequest(CodeErreur.CHAMP_MANQUANT,
                "L'en-tete « " + erreur.getHeaderName() + " » est obligatoire.");
    }

    /** Violation de contrainte hors corps de requete (parametres de chemin). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ReponseErreur> gererContrainte(ConstraintViolationException erreur) {
        return badRequest(CodeErreur.VALEUR_INVALIDE, "Une valeur transmise est invalide.");
    }

    /** Corps illisible, JSON malforme, champ de type incoherent. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReponseErreur> gererCorpsIllisible(HttpMessageNotReadableException erreur) {
        return badRequest(CodeErreur.VALEUR_INVALIDE, "Le corps de la requete est illisible ou mal forme.");
    }

    /** Parametre ou variable de chemin d'un type inattendu ({@code /api/tableau?promotionId=abc}). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ReponseErreur> gererTypeInvalide(MethodArgumentTypeMismatchException erreur) {
        return badRequest(CodeErreur.VALEUR_INVALIDE,
                "La valeur transmise pour « " + erreur.getName() + " » a un format invalide.");
    }

    /** Route inconnue : meme format d'erreur que le reste de l'API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ReponseErreur> gererRouteInconnue(NoResourceFoundException erreur) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ReponseErreur.de(CodeErreur.RESSOURCE_INTROUVABLE, "La ressource demandee est introuvable."));
    }

    /** Verbe HTTP non supporte par la ressource. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ReponseErreur> gererVerbeNonSupporte(HttpRequestMethodNotSupportedException erreur) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ReponseErreur.de(CodeErreur.METHODE_NON_AUTORISEE,
                        "Cette operation n'est pas disponible avec ce verbe HTTP."));
    }

    /**
     * Filet de securite : toute exception non prevue devient un 500 au format
     * impose, avec un message generique. La trace est journalisee cote serveur,
     * jamais renvoyee au client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> gererErreurInattendue(Exception erreur) {
        log.error("Erreur inattendue lors du traitement d'une requete", erreur);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReponseErreur.de(CodeErreur.ERREUR_INTERNE, MESSAGE_INTERNE));
    }

    private ResponseEntity<ReponseErreur> badRequest(CodeErreur code, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ReponseErreur.de(code, message));
    }
}
