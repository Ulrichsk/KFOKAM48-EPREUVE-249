package fr.kfokam48.tableau;

import java.util.List;

/**
 * Ligne du tableau de bord du formateur (schema {@code LigneTableau} du contrat).
 *
 * <p>Une ligne par etudiant de la promotion, meme sans aucune activite (Q16) — la
 * requete part de la liste des etudiants, jamais des donnees d'activite. Les cinq
 * champs derniers ({@code presences}, {@code presencesFormateur},
 * {@code exercicesDeposes}, {@code moyenne}, {@code relecturesEnAttente}) correspondent
 * au contrat impose. La moyenne est calculee cote API : elle n'est jamais recalculee
 * par le frontend, qui ne fait que l'afficher.</p>
 *
 * <p><b>Changement de besoin (enveloppe etape 3, issue 16)</b> : la ligne porte
 * desormais {@code exercicesSansRelecteur} — les identifiants des exercices de
 * l'etudiant restes {@code SANS_RELECTEUR} — pour que le formateur sache sur quelle
 * ligne designer un relecteur, sans quitter l'ecran. Champ additif, a l'image de
 * {@code presencesFormateur} (Q14).</p>
 *
 * <p>Le constructeur huit arguments est celui qu'utilise la projection JPQL
 * (Hibernate n'accepte qu'un agrégat a valeur unique par sous-requete de
 * construction) : la liste y vaut toujours {@code List.of()}, et
 * {@code TableauService} assemble ensuite les identifiants venus de la seconde
 * requete. Jamais {@code null} : un etudiant sans exercice bloque expose une liste
 * vide.</p>
 */
public record LigneTableau(

        Long etudiantId,

        String nom,

        String prenom,

        long presences,

        long presencesFormateur,

        long exercicesDeposes,

        double moyenne,

        long relecturesEnAttente,

        List<Long> exercicesSansRelecteur
) {

    public LigneTableau(Long etudiantId, String nom, String prenom, long presences,
                        long presencesFormateur, long exercicesDeposes, double moyenne,
                        long relecturesEnAttente) {
        this(etudiantId, nom, prenom, presences, presencesFormateur, exercicesDeposes,
                moyenne, relecturesEnAttente, List.of());
    }
}
