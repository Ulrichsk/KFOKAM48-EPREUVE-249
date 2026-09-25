package fr.kfokam48.relecture;

/**
 * Reponse de {@code POST /api/exercices/{id}/relecteur} :
 * {@code { id, exerciceId, statut, assignePar }}.
 *
 * <p>Exactement les quatre champs du contrat. Ni l'auteur de l'exercice, ni le lien, ni
 * aucune entite JPA (ENF3) : le formateur n'a pas besoin de connaitre l'auteur pour
 * debloquer un exercice sans relecteur.</p>
 *
 * <p>{@code assignePar} vaut toujours {@code FORMATEUR} ici, mais le champ est renvoye
 * tel quel par le contrat : c'est lui qui rend le deblocage manuel distinguable d'un
 * tirage au sort ({@code SYSTEME}) dans le tableau.</p>
 */
public record RelectureAssignee(

        Long id,

        Long exerciceId,

        StatutRelecture statut,

        AssignePar assignePar
) {

    public static RelectureAssignee depuis(Relecture relecture) {
        return new RelectureAssignee(
                relecture.getId(),
                relecture.getExerciceId(),
                relecture.getStatut(),
                relecture.getAssignePar());
    }
}
