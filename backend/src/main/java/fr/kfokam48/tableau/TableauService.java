package fr.kfokam48.tableau;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import fr.kfokam48.referentiel.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord du formateur (EF12, Q16).
 *
 * <p><b>Un nombre fixe de requetes</b>, sans boucle par etudiant (ENF8) : la
 * projection agregee ramene les lignes, la seconde requete ramene les couples
 * (etudiant, exercice sans relecteur), puis l'assemblage se fait en memoire sur une
 * map. Le cout ne depend pas du nombre d'etudiants.</p>
 *
 * <p><b>Une ligne par etudiant, toujours.</b> La base du tableau est la liste des
 * etudiants de la promotion (Q16) : un etudiant sans presence, sans depot et sans note
 * reste visible avec des zeros (et une moyenne nulle, RG18), il n'est jamais absent du
 * tableau parce qu'il n'a rien fait.</p>
 */
@Service
public class TableauService {

    private final PromotionRepository promotionRepository;
    private final TableauRepository tableauRepository;

    public TableauService(PromotionRepository promotionRepository,
                          TableauRepository tableauRepository) {
        this.promotionRepository = promotionRepository;
        this.tableauRepository = tableauRepository;
    }

    /**
     * Construit le tableau d'une promotion.
     *
     * <p><b>Changement de besoin (enveloppe etape 3, issue 16)</b> : chaque ligne
     * expose desormais {@code exercicesSansRelecteur} — les identifiants des exercices
     * de l'etudiant restes {@code SANS_RELECTEUR} — pour que le formateur designe un
     * relecteur directement sur la bonne ligne, sans quitter l'ecran. L'assemblage se
     * fait en memoire : les etudiants sans exercice bloque exposent une liste vide,
     * jamais {@code null}.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code PROMOTION_INCONNUE}
     */
    @Transactional(readOnly = true)
    public List<LigneTableau> tableauDeLaPromotion(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new RessourceIntrouvableException(
                    CodeErreur.PROMOTION_INCONNUE,
                    "La promotion demandee est inconnue.");
        }

        List<LigneTableau> lignes = tableauRepository.lignesDeLaPromotion(promotionId);

        Map<Long, List<Long>> bloquesParEtudiant = new HashMap<>();
        for (Object[] couple : tableauRepository.exercicesSansRelecteurDeLaPromotion(promotionId)) {
            bloquesParEtudiant
                    .computeIfAbsent((Long) couple[0], id -> new java.util.ArrayList<>())
                    .add((Long) couple[1]);
        }

        return lignes.stream()
                .map(ligne -> new LigneTableau(
                        ligne.etudiantId(),
                        ligne.nom(),
                        ligne.prenom(),
                        ligne.presences(),
                        ligne.presencesFormateur(),
                        ligne.exercicesDeposes(),
                        ligne.moyenne(),
                        ligne.relecturesEnAttente(),
                        bloquesParEtudiant.getOrDefault(ligne.etudiantId(), List.of())))
                .toList();
    }
}
