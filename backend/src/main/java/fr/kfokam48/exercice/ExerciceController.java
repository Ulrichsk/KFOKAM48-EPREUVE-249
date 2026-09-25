package fr.kfokam48.exercice;

import fr.kfokam48.relecture.DemandeAssignationRelecteur;
import fr.kfokam48.relecture.RelectureAssignee;
import fr.kfokam48.relecture.RelectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
 *
 * <p>Pas de {@code @RequestMapping} de classe : le contrat place la consultation des
 * exercices d'un etudiant sous {@code /api/etudiants/{id}/exercices} (EF11), hors du
 * préfixe commun — chaque méthode porte donc son chemin complet, comme dans
 * {@code PresenceController}.</p>
 */
@RestController
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
    @PostMapping("/api/exercices")
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
    @PostMapping("/api/exercices/{id}/relecteur")
    @ResponseStatus(HttpStatus.CREATED)
    public RelectureAssignee assignerUnRelecteur(@PathVariable Long id,
                                                @Valid @RequestBody DemandeAssignationRelecteur demande) {
        return relectureService.assignerUnRelecteur(id, demande.relecteurId());
    }

    /**
     * {@code PUT /api/exercices/{id}} — l'auteur remplace son lien tant que la
     * relecture n'a pas commence (EF6, Q13, RG16). Le contrat impose le header
     * {@code X-Etudiant-Id} : il designe l'appelant, que le service confronte a
     * l'auteur (403 sinon) et a l'horodatage de consultation (409 si la relecture a
     * deja commence). Reponse 200 avec le DTO {@code ExerciceDetail} du contrat.
     */
    @PutMapping("/api/exercices/{id}")
    public ExerciceDetail remplacerLien(@PathVariable Long id,
                                        @RequestHeader("X-Etudiant-Id") Long etudiantId,
                                        @Valid @RequestBody DemandeRemplacementLien demande) {
        return exerciceService.remplacerLien(id, etudiantId, demande.lien());
    }

    /**
     * {@code GET /api/etudiants/{id}/exercices} — l'etudiant relu voit sa note et son
     * commentaire (EF11, Q8). Le header {@code X-Etudiant-Id} doit designer le meme
     * etudiant que le chemin (403 sinon). La reponse ne contient aucune information
     * sur le relecteur : l'anonymat est porte par le DTO {@code ExerciceEtudiant}.
     */
    @GetMapping("/api/etudiants/{id}/exercices")
    public List<ExerciceEtudiant> listerMesExercices(
            @PathVariable("id") Long etudiantId,
            @RequestHeader("X-Etudiant-Id") Long appelantId) {
        return exerciceService.listerExercicesDeLetudiant(etudiantId, appelantId);
    }
}
