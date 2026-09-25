package fr.kfokam48.presence;

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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Regles metier du marquage de presence (EF2, EF3, EF13).
 *
 * <p>L'ordre des controles suit {@code docs/diagrammes/D3.md} et chacune de ces
 * etapes a une consequence observable par l'utilisateur :</p>
 * <ol>
 *   <li><b>etudiant</b> : un identifiant inconnu repond {@code 404 ETUDIANT_INCONNU} ;</li>
 *   <li><b>blocage</b> (RG5) : un etudiant bloque repond {@code 429 TROP_DE_TENTATIVES},
 *       meme avec le bon code, sinon il suffirait de continuer a essayer ;</li>
 *   <li><b>code inconnu</b> : {@code 400 CODE_INCONNU}, et la tentative est comptee ;</li>
 *   <li><b>session close</b> (RG21) : {@code 409 SESSION_CLOTUREE} — ce n'est pas une
 *       erreur de saisie, la tentative n'est donc pas comptee ;</li>
 *   <li><b>expiration</b> (RG2, RG3) : {@code 410 CODE_EXPIRE}, et la tentative est
 *       comptee ; l'expiration est verifiee <b>avant</b> l'unicite, pour qu'un code
 *       mort reponde {@code 410} et jamais {@code 409} ;</li>
 *   <li><b>promotion</b> (RG19) : {@code 403 ACCES_REFUSE} ;</li>
 *   <li><b>unicite</b> (RG4) : {@code 409 DEJA_PRESENT} ;</li>
 *   <li><b>succes</b> : la presence est creee et le compteur d'echecs est remis a zero.</li>
 * </ol>
 */
@Service
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final SessionRepository sessionRepository;
    private final EtudiantRepository etudiantRepository;
    private final CompteurTentativesCode compteurTentatives;
    private final Clock horloge;

    public PresenceService(PresenceRepository presenceRepository,
                           SessionRepository sessionRepository,
                           EtudiantRepository etudiantRepository,
                           CompteurTentativesCode compteurTentatives,
                           Clock horloge) {
        this.presenceRepository = presenceRepository;
        this.sessionRepository = sessionRepository;
        this.etudiantRepository = etudiantRepository;
        this.compteurTentatives = compteurTentatives;
        this.horloge = horloge;
    }

    /**
     * Marque la presence d'un etudiant pour la session dont le code est fourni.
     *
     * @throws RessourceIntrouvableException 404 {@code ETUDIANT_INCONNU}
     * @throws ErreurMetier 429 {@code TROP_DE_TENTATIVES} si l'etudiant est bloque (RG5)
     * @throws ErreurMetier 400 {@code CODE_INCONNU} si aucun session ne porte ce code
     * @throws ErreurMetier 409 {@code SESSION_CLOTUREE} si le formateur a clos la session
     * @throws ErreurMetier 410 {@code CODE_EXPIRE} si le code a depasse sa validite (RG2)
     * @throws ErreurMetier 403 {@code ACCES_REFUSE} si l'etudiant n'est pas de la promotion (RG19)
     * @throws ErreurMetier 409 {@code DEJA_PRESENT} si la presence existe deja (RG4)
     */
    @Transactional
    public PresenceCreee marquerPresence(DemandePresenceParCode demande) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);

        Etudiant etudiant = etudiantRepository.findById(demande.etudiantId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.ETUDIANT_INCONNU,
                        "L'etudiant demande est inconnu."));

        // RG5 : le blocage est verifie avant tout le reste (Q4).
        compteurTentatives.verifierNonBloque(etudiant.getId(), maintenant);

        Optional<Session> sessionTrouvee = sessionRepository.findByCode(normaliserCode(demande.code()));
        if (sessionTrouvee.isEmpty()) {
            compteurTentatives.enregistrerEchec(etudiant.getId(), maintenant);
            throw new ErreurMetier(
                    CodeErreur.CODE_INCONNU,
                    HttpStatus.BAD_REQUEST,
                    "Le code de presence est inconnu.");
        }
        Session session = sessionTrouvee.get();

        // RG21 : apres cloture, plus aucune presence n'est acceptee. Erreur de saisie ?
        // Non : c'est un etat de la session, donc la tentative n'est pas comptee.
        if (session.estCloturee()) {
            throw new ErreurMetier(
                    CodeErreur.SESSION_CLOTUREE,
                    HttpStatus.CONFLICT,
                    "Cette session est close : la presence ne peut plus etre marquee.");
        }

        // RG2 / RG3 : le code n'est valable que 15 minutes apres l'ouverture (Q2, Q3).
        if (session.codeExpireA(maintenant)) {
            compteurTentatives.enregistrerEchec(etudiant.getId(), maintenant);
            throw new ErreurMetier(
                    CodeErreur.CODE_EXPIRE,
                    HttpStatus.GONE,
                    "Le code de presence a expire.");
        }

        // RG19 : un etudiant ne marque sa presence que sur une session de sa promotion.
        if (!Objects.equals(etudiant.getPromotionId(), session.getPromotionId())) {
            throw new ErreurMetier(
                    CodeErreur.ACCES_REFUSE,
                    HttpStatus.FORBIDDEN,
                    "Cet etudiant n'appartient pas a la promotion de la session.");
        }

        // RG4 : message clair dans le cas courant ; la contrainte d'unicite de la
        // table reste la garante de l'invariant, ce que traduit le catch ci-dessous.
        if (presenceRepository.existsBySession_IdAndEtudiant_Id(session.getId(), etudiant.getId())) {
            throw dejaPresent();
        }

        Presence presence;
        try {
            presence = presenceRepository.saveAndFlush(
                    new Presence(session, etudiant, SourcePresence.ETUDIANT, maintenant));
        } catch (DataIntegrityViolationException conflit) {
            // Deux requetes simultanees pour le meme couple (session, etudiant) :
            // la base refuse la seconde ligne, on renvoie le meme 409 que la
            // verification applicative.
            throw dejaPresent();
        }

        // Q4 : une reussite repart d'un compteur vierge.
        compteurTentatives.reinitialiser(etudiant.getId(), maintenant);

        return PresenceCreee.depuis(presence);
    }

    /**
     * Le code est dicte puis recopie a la main : les espaces superflus et la casse
     * ne doivent pas faire echouer une saisie correcte (hypothese H9).
     */
    private String normaliserCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private ErreurMetier dejaPresent() {
        return new ErreurMetier(
                CodeErreur.DEJA_PRESENT,
                HttpStatus.CONFLICT,
                "Cet etudiant a deja marque sa presence pour cette session.");
    }
}
