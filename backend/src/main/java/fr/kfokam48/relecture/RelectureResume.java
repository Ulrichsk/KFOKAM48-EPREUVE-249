package fr.kfokam48.relecture;

import java.time.LocalDateTime;

/**
 * Element de la liste des relectures d'un etudiant (schema {@code RelectureResume} du
 * contrat) : {@code { id, exerciceId, statut, assigneAt }}.
 *
 * <p>Anonyme vis-a-vis de l'auteur (Q8, RG20) : ni le nom, ni l'identifiant de l'auteur
 * de l'exercice n'apparaissent — le relecteur n'a pas a savoir qui il juge. Aucune
 * entite JPA n'est exposee (ENF3).</p>
 */
public record RelectureResume(

        Long id,

        Long exerciceId,

        StatutRelecture statut,

        LocalDateTime assigneAt
) {

    public static RelectureResume depuis(Relecture relecture) {
        return new RelectureResume(
                relecture.getId(),
                relecture.getExerciceId(),
                relecture.getStatut(),
                relecture.getAssigneAt());
    }
}
