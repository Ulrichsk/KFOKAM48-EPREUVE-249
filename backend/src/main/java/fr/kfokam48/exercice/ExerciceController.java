package fr.kfokam48.exercice;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints des exercices.
 *
 * <p>Couche HTTP seule : validation de forme puis delegation. Aucun acces base, aucune
 * entite JPA en sortie (ENF3).</p>
 */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    /**
     * {@code POST /api/exercices} — depose le lien d'un exercice (EF5).
     *
     * <p>Le contrat impose {@code 201} avec {@code { id, statut }}, {@code 400 LIEN_INVALIDE}
     * et {@code 409 EXERCICE_DEJA_DEPOSE}. Les cas {@code 403 ACCES_REFUSE} (RG19),
     * {@code 409 SESSION_CLOTUREE} (RG15/RG21) et les {@code 404} sont documentes dans
     * {@code api/contrat.yaml}.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciceCree deposer(@Valid @RequestBody DemandeDepotExercice demande) {
        return exerciceService.deposer(demande);
    }
}
