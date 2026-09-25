package fr.kfokam48.relecture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de consultation des relectures (EF9).
 *
 * <p>Couche HTTP seule : le header {@code X-Etudiant-Id} declare l'appelant (Q1 : pas
 * d'authentification), le service verifie qu'il s'agit bien du relecteur designe. Aucun
 * acces base ici, aucune entite JPA en sortie (ENF3).</p>
 */
@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    /**
     * {@code GET /api/relectures} — liste les relectures confiees a l'etudiant appelant.
     *
     * <p>Anonymat de l'auteur garanti par le DTO (Q8, RG20).</p>
     */
    @GetMapping
    public List<RelectureResume> listerMesRelectures(
            @RequestHeader("X-Etudiant-Id") Long etudiantId) {
        return relectureService.listerRelecturesDeLetudiant(etudiantId);
    }

    /**
     * {@code GET /api/relectures/{id}} — le relecteur designe ouvre le lien a relire.
     *
     * <p>La premiere consultation horodate {@code lienConsulteAt}, ce qui fige le
     * remplacement du lien par l'auteur (Q13, RG16).</p>
     */
    @GetMapping("/{id}")
    public RelectureDetail consulterMaRelecture(
            @PathVariable Long id,
            @RequestHeader("X-Etudiant-Id") Long etudiantId) {
        return relectureService.consulterRelecture(id, etudiantId);
    }

    /**
     * {@code POST /api/relectures/{id}} — le relecteur designe rend sa note et son
     * commentaire (EF10). Operation imposee du contrat : note entiere 0-20 (Q9),
     * definitive des l'envoi (Q15) — toute seconde tentative repond 409.
     */
    @PostMapping("/{id}")
    public RelectureRendue rendreMaRelecture(
            @PathVariable Long id,
            @RequestHeader("X-Etudiant-Id") Long etudiantId,
            @RequestBody DemandeRelecture demande) {
        return relectureService.rendreRelecture(id, etudiantId, demande);
    }
}
