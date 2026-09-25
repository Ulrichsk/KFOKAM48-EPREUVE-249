package fr.kfokam48.presence;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Endpoints des presences.
 *
 * <p>Couche HTTP seule : validation de forme puis delegation au service. Aucune
 * requete base ici, aucune entite JPA en sortie (ENF3).</p>
 *
 * <p>Deux chemins pour deux origines (Q14, RG17) : {@code POST /api/presences} pour
 * l'etudiant qui saisit le code ({@code source = ETUDIANT}),
 * {@code POST /api/sessions/{id}/presences} pour l'ajout a la main du formateur
 * ({@code source = FORMATEUR}) — le contrat place ce dernier sous les sessions, comme
 * le tableau, qui doit distinguer les deux origines.</p>
 */
@RestController
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
    @PostMapping("/api/presences")
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceCreee marquerPresence(@Valid @RequestBody DemandePresenceParCode demande) {
        return presenceService.marquerPresence(demande);
    }

    /**
     * {@code POST /api/sessions/{id}/presences} — ajout manuel par le formateur (EF4,
     * Q14) : la presence creee porte {@code source = FORMATEUR} et reste soumise aux
     * memes garde-fous qu'une saisie etudiant (clôture, promotion, unicite).
     */
    @PostMapping("/api/sessions/{id}/presences")
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceCreee ajouterPresenceManuelle(@PathVariable Long id,
                                                 @Valid @RequestBody DemandePresenceManuelle demande) {
        return presenceService.ajouterPresenceManuelle(id, demande.etudiantId());
    }
}
