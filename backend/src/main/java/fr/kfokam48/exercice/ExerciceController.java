package fr.kfokam48.exercice;

import fr.kfokam48.relecture.DemandeAssignationRelecteur;
import fr.kfokam48.relecture.RelectureAssignee;
import fr.kfokam48.relecture.RelectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
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
 *
 * <p>Deux responsabilites sont deleguees a deux services distincts : le depot appartient
 * aux regles de l'exercice ({@link ExerciceService}), la designation d'un relecteur au
 * cycle de vie de la relecture ({@link RelectureService}). Le chemin reste celui des
 * exercices, comme le fixe le contrat.</p>
 */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;
    private final RelectureService relectureService;

    public ExerciceController(ExerciceService exerciceService, RelectureService relectureService) {
        this.exerciceService = exerciceService;
        this.relectureService = relectureService;
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

    /**
     * {@code POST /api/exercices/{id}/relecteur} — designe un relecteur a la main (EF8, Q11).
     *
     * <p>Voie de secours du tirage au sort : elle n'existe que pour les exercices restes
     * {@code SANS_RELECTEUR} (RG10, decision 7.4). Le contrat attend {@code 201} avec
     * {@code { id, exerciceId, statut, assignePar }}, {@code 400 CHAMP_MANQUANT},
     * {@code 403 AUTO_EVALUATION_INTERDITE} ou {@code ACCES_REFUSE}, et {@code 409
     * RELECTURE_DEJA_ASSIGNEE} ou {@code RELECTURE_DEJA_RENDUE}.</p>
     */
    @PostMapping("/{id}/relecteur")
    @ResponseStatus(HttpStatus.CREATED)
    public RelectureAssignee assignerUnRelecteur(@PathVariable Long id,
                                                @Valid @RequestBody DemandeAssignationRelecteur demande) {
        return relectureService.assignerUnRelecteur(id, demande.relecteurId());
    }
}
