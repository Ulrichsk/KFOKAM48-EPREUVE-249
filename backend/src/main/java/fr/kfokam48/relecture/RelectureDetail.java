package fr.kfokam48.relecture;

import fr.kfokam48.exercice.Exercice;

import java.time.LocalDateTime;

/**
 * Reponse de {@code GET /api/relectures/{id}} (schema {@code RelectureDetail} du
 * contrat) : ce qu'il faut pour relire — lien, session, statut — et rien de plus.
 *
 * <p>Anonymat garanti par construction (Q8, RG20) : ni le nom de l'auteur, ni celui du
 * relecteur, ni aucun identifiant permettant de les deduire. Le champ facultatif
 * {@code lienConsulteAt} montre au relecteur que le lien est deja fige (Q13).</p>
 */
public record RelectureDetail(

        Long id,

        Long exerciceId,

        StatutRelecture statut,

        String lien,

        String titreSession,

        LocalDateTime lienConsulteAt
) {

    public static RelectureDetail depuis(Relecture relecture) {
        Exercice exercice = relecture.getExercice();
        return new RelectureDetail(
                relecture.getId(),
                relecture.getExerciceId(),
                relecture.getStatut(),
                exercice.getLien(),
                exercice.getSession().getTitre(),
                relecture.getLienConsulteAt());
    }
}
