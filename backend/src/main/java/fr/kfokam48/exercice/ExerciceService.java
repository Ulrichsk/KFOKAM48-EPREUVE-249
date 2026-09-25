package fr.kfokam48.exercice;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.ErreurMetier;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.referentiel.EtudiantRepository;
import fr.kfokam48.session.Session;
import fr.kfokam48.session.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Regles metier du depot d'exercice (EF5).
 *
 * <p>Ordre des controles et justification de chacun :</p>
 * <ol>
 *   <li><b>lien</b> (RG22) : validation pure, sans acces base, pour refuser une entree
 *       mal formee sans rien interroger ;</li>
 *   <li><b>session</b> et <b>etudiant</b> existants ({@code 404}) ;</li>
 *   <li><b>session non close</b> (RG21) : « depose apres la fin de la session ? Oui,
 *       jusqu'a ce que je cloture » (Q12) ;</li>
 *   <li><b>promotion</b> (RG19) ;</li>
 *   <li><b>unicite</b> (RG4 par analogie) : un exercice par etudiant et par session.</li>
 * </ol>
 *
 * <p>Point important de Q12 : <b>l'expiration du code de presence n'est jamais verifiee
 * ici</b>. Un etudiant qui n'a pas de connexion le soir meme doit pouvoir deposer son
 * travail jusqu'a la cloture (RG15), meme si le code a expire depuis longtemps.</p>
 */
@Service
public class ExerciceService {

    private final ExerciceRepository exerciceRepository;
    private final SessionRepository sessionRepository;
    private final EtudiantRepository etudiantRepository;
    private final ValidateurLien validateurLien;
    private final Clock horloge;

    public ExerciceService(ExerciceRepository exerciceRepository,
                           SessionRepository sessionRepository,
                           EtudiantRepository etudiantRepository,
                           ValidateurLien validateurLien,
                           Clock horloge) {
        this.exerciceRepository = exerciceRepository;
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.validateurLien = validateurLien;
        this.horloge = horloge;
    }

    /**
     * Depose le lien d'un exercice pour une session.
     *
     * <p>Le statut renvoye est {@code EN_ATTENTE} : l'exercice attend une relecture. Le
     * tirage au sort du relecteur (issue 07) s'inserera dans cette transaction et pourra
     * le faire basculer en {@code SANS_RELECTEUR} lorsque aucun etudiant n'est eligible
     * (RG10).</p>
     *
     * @throws ErreurMetier 400 {@code LIEN_INVALIDE}
     * @throws RessourceIntrouvableException 404 {@code SESSION_INTROUVABLE} ou {@code ETUDIANT_INCONNU}
     * @throws ErreurMetier 409 {@code SESSION_CLOTUREE}
     * @throws ErreurMetier 403 {@code ACCES_REFUSE} si l'etudiant n'est pas de la promotion (RG19)
     * @throws ErreurMetier 409 {@code EXERCICE_DEJA_DEPOSE} si un exercice existe deja
     */
    @Transactional
    public ExerciceCree deposer(DemandeDepotExercice demande) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);

        // RG22 : validation d'entree d'abord, sans toucher a la base.
        String lien = demande.lien() == null ? "" : demande.lien().trim();
        if (!validateurLien.estValide(lien)) {
            throw new ErreurMetier(
                    CodeErreur.LIEN_INVALIDE,
                    HttpStatus.BAD_REQUEST,
                    "Le lien doit etre une URL absolue commencant par http:// ou https://, "
                            + "de " + ValidateurLien.LONGUEUR_MAX + " caracteres au plus.");
        }

        Session session = sessionRepository.findById(demande.sessionId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.SESSION_INTROUVABLE,
                        "La session demandee est inconnue."));

        Etudiant etudiant = etudiantRepository.findById(demande.etudiantId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.ETUDIANT_INCONNU,
                        "L'etudiant demande est inconnu."));

        // RG15 / RG21 : depot possible jusqu'a la cloture, jamais apres.
        if (session.estCloturee()) {
            throw new ErreurMetier(
                    CodeErreur.SESSION_CLOTUREE,
                    HttpStatus.CONFLICT,
                    "Cette session est close : aucun nouvel exercice ne peut etre depose.");
        }

        // RG19 : un etudiant ne depose que sur une session de sa promotion.
        if (!Objects.equals(etudiant.getPromotionId(), session.getPromotionId())) {
            throw new ErreurMetier(
                    CodeErreur.ACCES_REFUSE,
                    HttpStatus.FORBIDDEN,
                    "Cet etudiant n'appartient pas a la promotion de la session.");
        }

        if (exerciceRepository.existsBySession_IdAndEtudiant_Id(session.getId(), etudiant.getId())) {
            throw dejaDepose();
        }

        try {
            Exercice exercice = exerciceRepository.saveAndFlush(
                    new Exercice(session, etudiant, lien, StatutExercice.EN_ATTENTE, maintenant));
            return ExerciceCree.depuis(exercice);
        } catch (DataIntegrityViolationException conflit) {
            // La contrainte (session_id, etudiant_id) a refuse une seconde ligne
            // simultanee : meme reponse que la verification applicative.
            throw dejaDepose();
        }
    }

    private ErreurMetier dejaDepose() {
        return new ErreurMetier(
                CodeErreur.EXERCICE_DEJA_DEPOSE,
                HttpStatus.CONFLICT,
                "Un exercice a deja ete depose pour cette session.");
    }
}
