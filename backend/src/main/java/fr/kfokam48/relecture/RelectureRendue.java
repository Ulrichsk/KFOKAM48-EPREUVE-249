package fr.kfokam48.relecture;

import java.time.LocalDateTime;

/**
 * Reponse de {@code POST /api/relectures/{id}} (schema {@code RelectureRendue} du
 * contrat) : {@code { id, exerciceId, statut, note, commentaire, renduAt }}.
 *
 * <p>Aucune entite JPA exposee (ENF3) ; aucun identifiant de relecteur — cette reponse
 * pourra etre relue par l'auteur plus tard sans rien devoiler (Q8, RG20).</p>
 */
public record RelectureRendue(

        Long id,

        Long exerciceId,

        StatutRelecture statut,

        Integer note,

        String commentaire,

        LocalDateTime renduAt
) {

    public static RelectureRendue depuis(Relecture relecture) {
        return new RelectureRendue(
                relecture.getId(),
                relecture.getExerciceId(),
                relecture.getStatut(),
                relecture.getNote(),
                relecture.getCommentaire(),
                relecture.getRenduAt());
    }
}
