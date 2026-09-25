package fr.kfokam48.relecture;

import fr.kfokam48.presence.PresenceRepository;
import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.session.Session;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Designation du relecteur d'un exercice (Q6, Q7).
 *
 * <p>Deux regles encadrent le tirage, et une seule requete suffit a les appliquer :</p>
 * <ul>
 *   <li><b>qui est eligible</b> (RG8) : les etudiants ayant une presence a la session de
 *       l'exercice, <b>quelle que soit la source</b> de cette presence. Un etudiant ajoute
 *       a la main par le formateur parce que son telephone ne fonctionnait pas (Q14) reste
 *       donc eligible — l'exclure le sanctionnerait une seconde fois pour un incident
 *       materiel (decision 7.2 du cahier des charges) ;</li>
 *   <li><b>qui est exclu</b> (RG6) : l'auteur de l'exercice, par principe (Q5 : « Jamais.
 *       C'est le principe meme. »).</li>
 * </ul>
 *
 * <p>Cette classe est appelee <b>dans la transaction de depot</b> : elle ne declare donc
 * aucune transaction propre (decision 7.3 : le tirage a lieu immediatement au depot, dans
 * la meme transaction que la creation de l'exercice et de la relecture).</p>
 */
@Component
public class TirageRelecteur {

    private final PresenceRepository presenceRepository;
    private final TirageAuSort tirageAuSort;

    public TirageRelecteur(PresenceRepository presenceRepository, TirageAuSort tirageAuSort) {
        this.presenceRepository = presenceRepository;
        this.tirageAuSort = tirageAuSort;
    }

    /**
     * Tire un relecteur parmi les presents eligibles.
     *
     * @return le relecteur designe, ou vide si aucun etudiant n'est eligible — ce qui
     *         conduit l'exercice au statut {@code SANS_RELECTEUR} (RG10)
     */
    public Optional<Etudiant> tirer(Session session, Etudiant auteur) {
        List<Etudiant> eligibles =
                presenceRepository.findEligiblesPourTirage(session.getId(), auteur.getId());
        return tirageAuSort.choisirParmi(eligibles);
    }
}
