package fr.kfokam48.presence;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints des presences.
 *
 * <p>Couche HTTP seule : validation de forme puis delegation au service. Aucune
 * requete base ici, aucune entite JPA en sortie (ENF3).</p>
 */
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * {@code POST /api/presences} — marque la presence avec le code de la session (EF2).
     *
     * <p>Le contrat impose {@code 201} en succes, {@code 400 CODE_INCONNU},
     * {@code 409 DEJA_PRESENT} et {@code 410 CODE_EXPIRE}. Les cas
     * {@code 403 ACCES_REFUSE} (RG19) et {@code 404 ETUDIANT_INCONNU} sont ajoutes et
     * documentes dans {@code api/contrat.yaml}.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceCreee marquerPresence(@Valid @RequestBody DemandePresenceParCode demande) {
        return presenceService.marquerPresence(demande);
    }
}
