package fr.kfokam48.tableau;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code GET /api/tableau?promotionId=} — tableau de bord du formateur (EF12, Q16).
 *
 * <p>Operation imposee du contrat : 200 avec une ligne par etudiant de la promotion,
 * 404 {@code PROMOTION_INCONNUE} sinon. Le parametre {@code promotionId} est obligatoire ;
 * son absence est traduite en {@code 400 CHAMP_MANQUANT} par le gestionnaire central
 * (ENF4). Couche HTTP seule : delegation, aucune requete base ici, aucune entite JPA
 * en sortie (ENF3).</p>
 */
@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @GetMapping
    public List<LigneTableau> tableau(@RequestParam Long promotionId) {
        return tableauService.tableauDeLaPromotion(promotionId);
    }
}
