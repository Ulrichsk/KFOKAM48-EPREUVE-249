package fr.kfokam48.session;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints des sessions de cours.
 *
 * <p>Couche HTTP : validation de forme puis delegation. Aucun acces base, aucune
 * entite JPA en reponse (ENF3).</p>
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * {@code POST /api/sessions} — ouvre une session et renvoie son code (EF1).
     *
     * <p>Le contrat impose {@code 201} en succes, {@code 400 CHAMP_MANQUANT} si un
     * champ manque et {@code 404 PROMOTION_INCONNUE} si la promotion n'existe pas.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionCreee ouvrirSession(@Valid @RequestBody DemandeOuvertureSession demande) {
        return sessionService.ouvrirSession(demande);
    }
}
