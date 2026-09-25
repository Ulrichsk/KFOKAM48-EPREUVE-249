package fr.kfokam48.presence;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.ErreurMetier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Blocage des tentatives de devinette du code (RG5, Q4).
 *
 * <p>Ce service existe pour une raison technique precise : quand une tentative
 * echoue, {@code PresenceService} leve une exception metier — ce qui fait
 * <b>annuler</b> sa transaction. Si le compteur etait incremente dans cette meme
 * transaction, l'increment serait perdu et le blocage ne se declencherait jamais.
 * Les trois operations ci-dessous s'executent donc dans une transaction
 * separee ({@link Propagation#REQUIRES_NEW}) qui est validee independamment de
 * l'echec de la tentative.</p>
 */
@Service
public class CompteurTentativesCode {

    private final TentativeCodeRepository tentativeCodeRepository;

    public CompteurTentativesCode(TentativeCodeRepository tentativeCodeRepository) {
        this.tentativeCodeRepository = tentativeCodeRepository;
    }

    /**
     * Refuse la tentative si l'etudiant est encore bloque.
     *
     * <p>Appelee <b>avant</b> toute autre verification : un etudiant bloque ne doit
     * pas pouvoir progresser, meme en saisissant le bon code (Q4).</p>
     *
     * @throws ErreurMetier 429 {@code TROP_DE_TENTATIVES}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void verifierNonBloque(Long etudiantId, LocalDateTime maintenant) {
        tentativeCodeRepository.findByEtudiantId(etudiantId)
                .map(TentativeCode::etat)
                .filter(etat -> etat.bloqueA(maintenant))
                .ifPresent(etat -> {
                    throw new ErreurMetier(
                            CodeErreur.TROP_DE_TENTATIVES,
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Trop de tentatives : la saisie du code est bloquee pendant deux minutes.");
                });
    }

    /**
     * Enregistre une tentative infructueuse et declenche le blocage si le seuil est
     * atteint (Q4). Validee immediatement, pour survivre a l'echec de la tentative.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerEchec(Long etudiantId, LocalDateTime maintenant) {
        TentativeCode compteur = tentativeCodeRepository.findByEtudiantId(etudiantId)
                .orElseGet(() -> new TentativeCode(etudiantId, maintenant));
        compteur.appliquer(compteur.etat().apresEchec(maintenant), maintenant);
        tentativeCodeRepository.saveAndFlush(compteur);
    }

    /** Une tentative reussie efface les echecs consecutifs et tout blocage (Q4). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reinitialiser(Long etudiantId, LocalDateTime maintenant) {
        tentativeCodeRepository.findByEtudiantId(etudiantId).ifPresent(compteur -> {
            compteur.appliquer(EtatTentativesCode.initial(), maintenant);
            tentativeCodeRepository.saveAndFlush(compteur);
        });
    }
}
