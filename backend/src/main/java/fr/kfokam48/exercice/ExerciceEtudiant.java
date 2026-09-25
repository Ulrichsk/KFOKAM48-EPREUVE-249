package fr.kfokam48.exercice;

/**
 * Exercice vu par son auteur (schema {@code ExerciceEtudiant} du contrat) :
 * {@code { id, sessionId, titreSession, statut, note, commentaire }}.
 *
 * <p><b>Anonymat du relecteur par construction</b> (Q8, RG20) : ce DTO ne contient — et
 * ne peut pas contenir — d'identifiant de relecteur, de nom, ni de date d'assignation
 * permettant de le deduire. La note et le commentaire ne sont renseignes qu'une fois
 * la relecture rendue (Q15) ; avant, ils restent absents.</p>
 */
public record ExerciceEtudiant(

        Long id,

        Long sessionId,

        String titreSession,

        StatutExercice statut,

        Integer note,

        String commentaire
) {

    /**
     * Assemble la vue auteur depuis l'exercice et, s'il existe, sa relecture. Une
     * relecture en attente ne divulgue rien : note et commentaire restent nuls tant
     * qu'elle n'est pas rendue.
     */
    public static ExerciceEtudiant depuis(Exercice exercice, RelectureVue relecture) {
        return new ExerciceEtudiant(
                exercice.getId(),
                exercice.getSessionId(),
                exercice.getSession().getTitre(),
                exercice.getStatut(),
                relecture == null ? null : relecture.note(),
                relecture == null ? null : relecture.commentaire());
    }

    /** Projection minimale de la relecture rendue, sans aucune identite (RG20). */
    public record RelectureVue(Integer note, String commentaire) {
    }
}
