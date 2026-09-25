package fr.kfokam48.referentiel;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regles metier du referentiel (promotions, etudiants).
 *
 * <p>Toute la logique est ici : le controleur ne fait que deleguer, il n'accede
 * jamais a la base (ENF3).</p>
 */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;

    public PromotionService(PromotionRepository promotionRepository, EtudiantRepository etudiantRepository) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
    }

    /**
     * Liste des etudiants d'une promotion, triee par nom.
     *
     * <p>EF15 : le contrat impose un {@code 404} lorsque la promotion est
     * inconnue. On verifie donc l'existence de la promotion avant de renvoyer
     * une liste vide, sinon une promotion inexistante serait indiscernable d'une
     * promotion sans etudiant.</p>
     *
     * @throws RessourceIntrouvableException si la promotion n'existe pas
     */
    @Transactional(readOnly = true)
    public List<EtudiantDto> listerEtudiants(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new RessourceIntrouvableException(
                    CodeErreur.PROMOTION_INCONNUE,
                    "La promotion demandee est inconnue.");
        }
        return etudiantRepository.findByPromotion_IdOrderByNomAscPrenomAsc(promotionId)
                .stream()
                .map(EtudiantDto::depuis)
                .toList();
    }
}
