package fr.kfokam48.tableau;

import fr.kfokam48.referentiel.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Requetes agregees du tableau de bord (EF12, ENF8).
 *
 * <p>Une seule projection fait tout le travail : elle part de la liste des etudiants de
 * la promotion et y attache les agregats par sous-requetes correlees. Un etudiant sans
 * aucune activite reste donc present, avec des zeros et une moyenne nulle (Q16, RG18) —
 * partir des activites au lieu des etudiants ferait disparaitre les lignes vides, ce que
 * Q16 interdit. Quatre requetes au total, independantes du nombre d'etudiants : aucune
 * boucle de requetes par etudiant (ENF8).</p>
 *
 * <p>La moyenne est arrondie a une decimale par {@code ROUND(..., 1)} (RG18) et vaut
 * {@code null} — affichee ensuite comme 0 — lorsqu'aucune note n'existe. Les relectures
 * en attente comptent celles que l'etudiant doit encore rendre (Q11) ; les exercices
 * restes sans relecteur sont comptes a part dans la ligne du formateur via les statuts
 * d'exercices, et ne disparaissent jamais du tableau.</p>
 *
 * <p>La moyenne compte les notes <b>recues</b> : la relecture porte l'auteur de
 * l'exercice, pas le relecteur. Que le relecteur ne soit jamais l'auteur (Q5, RG6) est
 * deja garanti a l'ecriture par les deux voies d'assignation — la requete n'a donc pas
 * a le re-verifier.</p>
 */
public interface TableauRepository extends JpaRepository<Etudiant, Long> {

    @Query("""
            select new fr.kfokam48.tableau.LigneTableau(
                e.id,
                e.nom,
                e.prenom,
                coalesce((select count(p) from Presence p where p.etudiant = e), 0),
                coalesce((select count(p) from Presence p
                          where p.etudiant = e and p.source = fr.kfokam48.presence.SourcePresence.FORMATEUR), 0),
                coalesce((select count(x) from Exercice x where x.etudiant = e), 0),
                coalesce((select round(avg(r.note), 1) from Relecture r
                          where r.statut = fr.kfokam48.relecture.StatutRelecture.RENDUE
                            and r.exercice.etudiant = e), 0.0),
                coalesce((select count(r) from Relecture r
                          where r.relecteur = e
                            and r.statut = fr.kfokam48.relecture.StatutRelecture.EN_ATTENTE), 0)
            )
            from Etudiant e
            where e.promotion.id = :promotionId
            order by e.nom asc, e.prenom asc
            """)
    List<LigneTableau> lignesDeLaPromotion(@Param("promotionId") Long promotionId);
}
