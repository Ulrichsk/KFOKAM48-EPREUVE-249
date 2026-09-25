package fr.kfokam48.session;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.ErreurMetier;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import fr.kfokam48.referentiel.Promotion;
import fr.kfokam48.referentiel.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Regles metier des sessions de cours.
 *
 * <p>Le controleur ne fait que transmettre : toute la logique (generation du code,
 * calcul de l'expiration, controle de la promotion) est ici (ENF3).</p>
 */
@Service
public class SessionService {

    /**
     * RG1 : le code doit etre unique toutes sessions confondues, y compris parmi
     * les sessions closes. Sinon un code deja utilise ne designerait plus une
     * session unique, et « saisir le code » deviendrait ambigu (contrat :
     * {@code 400 CODE_INCONNU} suppose une resolution sans ambiguite).
     */
    private static final int TENTATIVES_GENERATION_CODE = 10;

    private final SessionRepository sessionRepository;
    private final PromotionRepository promotionRepository;
    private final GenerateurCodeSession generateurCode;
    private final Clock horloge;

    public SessionService(SessionRepository sessionRepository,
                          PromotionRepository promotionRepository,
                          GenerateurCodeSession generateurCode,
                          Clock horloge) {
        this.sessionRepository = sessionRepository;
        this.promotionRepository = promotionRepository;
        this.generateurCode = generateurCode;
        this.horloge = horloge;
    }

    /**
     * Ouvre une session et lui attribue un code de presence (EF1).
     *
     * @throws RessourceIntrouvableException si la promotion est inconnue
     */
    @Transactional
    public SessionCreee ouvrirSession(DemandeOuvertureSession demande) {
        Promotion promotion = promotionRepository.findById(demande.promotionId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.PROMOTION_INCONNUE,
                        "La promotion demandee est inconnue."));

        LocalDateTime ouvertureAt = LocalDateTime.now(horloge);
        Session session = new Session(promotion, demande.titre(), genererCodeUnique(), ouvertureAt);

        return SessionCreee.depuis(sessionRepository.save(session));
    }

    /**
     * Clôture une session (EF14, RG21) : ferme les depots et les presences.
     *
     * <p><b>L'operation est idempotente</b> (decision 7.5) : recloturer une session
     * deja close repond 200 avec la date de la premiere clôture, sans erreur — un
     * double-clic sur le bouton du formateur ne doit pas produire d'etat incoherent.
     * La clôture ne touche ni aux relectures en cours, ni aux notes : les relectures
     * deja assignees restent rendables apres la clôture (RG21) — {@code
     * RelectureService.rendreRelecture} ne consulte d'ailleurs jamais {@code clotureAt}.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code SESSION_INTROUVABLE}
     */
    @Transactional
    public SessionCloturee cloturerSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.SESSION_INTROUVABLE,
                        "La session demandee est inconnue."));

        // Idempotent : une session deja close conserve sa date de premiere clôture.
        session.cloturer(LocalDateTime.now(horloge));

        return SessionCloturee.depuis(sessionRepository.save(session));
    }

    /**
     * Tire un code jusqu'a en trouver un non utilise. L'echec au bout de dix
     * tentatives signale une anomalie technique (espace de codes sature ou
     * generateur defaillant), pas une erreur de l'utilisateur : le message reste
     * donc generique.
     */
    private String genererCodeUnique() {
        for (int tentative = 0; tentative < TENTATIVES_GENERATION_CODE; tentative++) {
            String code = generateurCode.generer();
            if (!sessionRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new ErreurMetier(
                CodeErreur.ERREUR_INTERNE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Impossible de generer un code de presence disponible.");
    }
}
