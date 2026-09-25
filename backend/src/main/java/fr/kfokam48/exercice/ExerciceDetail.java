package fr.kfokam48.exercice;

/**
 * Reponse de {@code PUT /api/exercices/{id}} (schema {@code ExerciceDetail} du
 * contrat) : {@code { id, sessionId, statut, lien, relectureCommencee }}.
 *
 * <p>Aucune entite JPA exposee (ENF3). {@code relectureCommencee} dit a l'auteur si le
 * relecteur a deja ouvert le lien (Q13) : tant qu'il vaut {@code false}, le lien peut
 * encore etre remplace ; des qu'il vaut {@code true}, l'operation est refusee (409).</p>
 */
public record ExerciceDetail(

        Long id,

        Long sessionId,

        StatutExercice statut,

        String lien,

        boolean relectureCommencee
) {

    public static ExerciceDetail depuis(Exercice exercice, boolean relectureCommencee) {
        return new ExerciceDetail(
                exercice.getId(),
                exercice.getSessionId(),
                exercice.getStatut(),
                exercice.getLien(),
                relectureCommencee);
    }
}
