package fr.kfokam48.tableau;

/**
 * Ligne du tableau de bord du formateur (schema {@code LigneTableau} du contrat).
 *
 * <p>Une ligne par etudiant de la promotion, meme sans aucune activite (Q16) — la
 * requete part de la liste des etudiants, jamais des donnees d'activite. Les cinq
 * champs derniers ({@code presences}, {@code presencesFormateur},
 * {@code exercicesDeposes}, {@code moyenne}, {@code relecturesEnAttente}) correspondent
 * au contrat impose. La moyenne est calculee cote API : elle n'est jamais recalculee
 * par le frontend, qui ne fait que l'afficher.</p>
 */
public record LigneTableau(

        Long etudiantId,

        String nom,

        String prenom,

        long presences,

        long presencesFormateur,

        long exercicesDeposes,

        double moyenne,

        long relecturesEnAttente
) {
}
