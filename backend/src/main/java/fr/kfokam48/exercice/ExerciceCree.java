package fr.kfokam48.exercice;

/**
 * Reponse de {@code POST /api/exercices} : {@code { id, statut }}.
 *
 * <p>Exactement les deux champs du contrat : ni le lien, ni l'auteur, ni aucune entite
 * JPA (ENF3).</p>
 */
public record ExerciceCree(Long id, StatutExercice statut) {

    public static ExerciceCree depuis(Exercice exercice) {
        return new ExerciceCree(exercice.getId(), exercice.getStatut());
    }
}
