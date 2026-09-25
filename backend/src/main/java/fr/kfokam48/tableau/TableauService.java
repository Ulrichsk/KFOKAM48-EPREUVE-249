package fr.kfokam48.tableau;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import fr.kfokam48.referentiel.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tableau de bord du formateur (EF12, Q16).
 *
 * <p><b>Une seule passe de requetes agregees</b>, sans boucle par etudiant (ENF8) :
 * quatre requetes ramenent l'activite de toute la promotion, puis l'assemblage se fait
 * en memoire sur des maps. Le cout ne depend donc pas du nombre d'etudiants.</p>
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

        return lignes;
    }
}
