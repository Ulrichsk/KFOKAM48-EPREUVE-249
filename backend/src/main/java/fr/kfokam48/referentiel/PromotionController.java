package fr.kfokam48.referentiel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints du referentiel.
 *
 * <p>Couche HTTP uniquement : validation du format d'entree et delegation au
 * service. Aucune requete base, aucun repository injecte, aucune entite JPA en
 * sortie (ENF3). Les erreurs remontent au {@code GestionnaireErreurs}.</p>
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * {@code GET /api/promotions/{id}/etudiants} — liste de noms de Q1 (EF15).
     */
    @GetMapping("/{id}/etudiants")
    public List<EtudiantDto> listerEtudiants(@PathVariable("id") Long promotionId) {
        return promotionService.listerEtudiants(promotionId);
    }
}
