package fr.kfokam48.relecture;

import fr.kfokam48.commun.erreur.CodeErreur;
import fr.kfokam48.commun.erreur.ErreurMetier;
import fr.kfokam48.commun.erreur.RessourceIntrouvableException;
import fr.kfokam48.exercice.Exercice;
import fr.kfokam48.exercice.ExerciceRepository;
import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.referentiel.EtudiantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Regles metier des relectures (EF8 a EF10).
 *
 * <p>Issue 08 : le deblocage manuel d'un exercice reste sans relecteur (RG10, decision
 * 7.4 du cahier des charges). Les issues 09 et 10 viendront ajouter ici la consultation
 * du lien et le rendu de la note : ce sont les etapes du meme cycle de vie.</p>
 *
 * <p><b>Ordre des controles.</b> Il suit celui deja retenu pour la presence et pour le
 * depot : ressource visee, puis <i>etat</i> de la ressource, puis <i>coherence</i> de la
 * demande. Cet ordre n'est pas cosmetique :</p>
 * <ol>
 *   <li><b>exercice</b> et <b>relecteur</b> connus, sinon {@code 404} ;</li>
 *   <li><b>l'exercice a-t-il deja un relecteur ?</b> ({@code 409}) — c'est un etat de la
 *       ressource, et il rend l'operation impossible <i>quelle que soit</i> la personne
 *       proposee : le dire avant de juger cette personne evite de faire croire a un
 *       probleme sur le relecteur choisi ;</li>
 *   <li><b>l'auteur</b> ({@code 403 AUTO_EVALUATION_INTERDITE}, RG6) : l'auto-evaluation
 *       est interdite par principe (Q5), par tirage comme a la main ;</li>
 *   <li><b>la promotion</b> ({@code 403 ACCES_REFUSE}) : le relecteur doit appartenir a
 *       la promotion de la session, par coherence avec RG19 ;</li>
 *   <li><b>succes</b> : la relecture est creee avec {@code assignePar = FORMATEUR} et
 *       l'exercice passe de {@code SANS_RELECTEUR} a {@code EN_ATTENTE}.</li>
 * </ol>
 *
 * <p><b>Ce qui n'est volontairement pas exige.</b> Le relecteur designe a la main n'a
 * pas besoin d'etre present a la session : l'operation existe precisement parce que le
 * tirage n'a trouve personne (§7.4). Exiger une presence rendrait l'endpoint inutile,
 * puisque le seul present est souvent l'auteur, de toute facon exclu par RG6. Seule
 * l'appartenance a la promotion est verifiee.</p>
 */
@Service
public class RelectureService {

    private final ExerciceRepository exerciceRepository;
    private final EtudiantRepository etudiantRepository;
    private final RelectureRepository relectureRepository;
    private final Clock horloge;

    public RelectureService(ExerciceRepository exerciceRepository,
                            EtudiantRepository etudiantRepository,
                            RelectureRepository relectureRepository,
                            Clock horloge) {
        this.exerciceRepository = exerciceRepository;
        this.etudiantRepository = etudiantRepository;
        this.relectureRepository = relectureRepository;
        this.horloge = horloge;
    }

    /**
     * Designe a la main le relecteur d'un exercice reste sans relecteur (EF8, Q11).
     *
     * <p>La relecture et le changement de statut de l'exercice appartiennent a la meme
     * transaction : on ne peut pas observer un exercice {@code EN_ATTENTE} sans relecture,
     * ni l'inverse.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code EXERCICE_INTROUVABLE} ou {@code ETUDIANT_INCONNU}
     * @throws ErreurMetier 409 {@code RELECTURE_DEJA_ASSIGNEE} si un relecteur est deja designe
     * @throws ErreurMetier 409 {@code RELECTURE_DEJA_RENDUE} si la note a deja ete envoyee
     * @throws ErreurMetier 403 {@code AUTO_EVALUATION_INTERDITE} si le relecteur est l'auteur (RG6)
     * @throws ErreurMetier 403 {@code ACCES_REFUSE} si le relecteur n'est pas de la promotion (RG19)
     */
    @Transactional
    public RelectureAssignee assignerUnRelecteur(Long exerciceId, Long relecteurId) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);

        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.EXERCICE_INTROUVABLE,
                        "L'exercice demande est inconnu."));

        Etudiant relecteur = etudiantRepository.findById(relecteurId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.ETUDIANT_INCONNU,
                        "L'etudiant demande est inconnu."));

        // Etat de la ressource : un exercice n'a qu'un relecteur (Q6, RG7). La contrainte
        // d'unicite de la table reste la garante de l'invariant.
        Optional<Relecture> existante = relectureRepository.findByExercice_Id(exerciceId);
        if (existante.isPresent()) {
            throw dejaAssignee(existante.get());
        }

        // RG6 / Q5 : « jamais » — un etudiant ne relit pas son propre exercice, y compris
        // quand c'est le formateur qui le designe.
        if (Objects.equals(relecteur.getId(), exercice.getEtudiantId())) {
            throw new ErreurMetier(
                    CodeErreur.AUTO_EVALUATION_INTERDITE,
                    HttpStatus.FORBIDDEN,
                    "Un etudiant ne peut pas relire son propre exercice.");
        }

        // RG19 par analogie : le relecteur est un pair de la meme promotion.
        if (!Objects.equals(relecteur.getPromotionId(), exercice.getSession().getPromotionId())) {
            throw new ErreurMetier(
                    CodeErreur.ACCES_REFUSE,
                    HttpStatus.FORBIDDEN,
                    "Cet etudiant n'appartient pas a la promotion de la session.");
        }

        // RG10 : l'exercice quitte SANS_RELECTEUR pour EN_ATTENTE. Le statut DEPOSE n'est
        // pas atteignable ici : il n'existe que le temps de la transaction de depot (D4).
        exercice.confierAUnRelecteur(maintenant);
        exerciceRepository.save(exercice);

        Relecture relecture = relectureRepository.saveAndFlush(
                new Relecture(exercice, relecteur, AssignePar.FORMATEUR, maintenant));

        return RelectureAssignee.depuis(relecture);
    }

    /**
     * Liste les relectures confiees a un etudiant (EF9), les plus recentes d'abord.
     *
     * <p>Anonymat de l'auteur garanti par le DTO (Q8, RG20) : la reponse ne contient
     * ni le nom ni l'identifiant de l'auteur de l'exercice.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code ETUDIANT_INCONNU}
     */
    @Transactional(readOnly = true)
    public List<RelectureResume> listerRelecturesDeLetudiant(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.ETUDIANT_INCONNU,
                        "L'etudiant demande est inconnu."));

        return relectureRepository.findByRelecteur_IdOrderByAssigneAtDesc(etudiant.getId()).stream()
                .map(RelectureResume::depuis)
                .toList();
    }

    /**
     * Le relecteur designe consulte le lien a relire (EF9).
     *
     * <p>Controles dans l'ordre du contrat : la relecture existe ({@code 404}), puis
     * l'appelant en est bien le relecteur designe ({@code 403 ACCES_REFUSE} — un autre
     * etudiant, y compris l'auteur lui-meme, n'a rien a faire ici).</p>
     *
     * <p><b>Horodatage fige.</b> La premiere consultation renseigne {@code
     * lienConsulteAt} : des lors, le lien n'est plus remplaçable par l'auteur (Q13,
     * RG16). Les consultations suivantes ne l'ecrasent pas : l'instant de la premiere
     * consultation fait foi pour trancher un litige sur le lien.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code RELECTURE_INTROUVABLE}
     * @throws ErreurMetier 403 {@code ACCES_REFUSE} si l'appelant n'est pas le relecteur
     */
    @Transactional
    public RelectureDetail consulterRelecture(Long relectureId, Long etudiantAppelant) {
        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.RELECTURE_INTROUVABLE,
                        "La relecture demandee est inconnue."));

        if (!Objects.equals(relecture.getRelecteurId(), etudiantAppelant)) {
            throw new ErreurMetier(
                    CodeErreur.ACCES_REFUSE,
                    HttpStatus.FORBIDDEN,
                    "Seul le relecteur designe peut consulter cette relecture.");
        }

        if (relecture.getLienConsulteAt() == null) {
            relecture.marquerLienConsulte(LocalDateTime.now(horloge));
        }

        return RelectureDetail.depuis(relecture);
    }

    /**
     * Le relecteur designe rend sa note et son commentaire (EF10).
     *
     * <p><b>Ordre des controles</b>, aligne sur la convention des autres endpoints :
     * ressource visee, identite de l'appelant, forme de la note, puis etat de la
     * relecture.</p>
     *
     * <p><b>La note est definitive des son envoi</b> (Q15, decision 7.1) : il n'existe
     * aucun chemin d'ecriture vers une relecture {@code RENDUE} — le second envoi est
     * refuse avant d'avoir pu modifier quoi que ce soit, et la note d'origine reste
     * inchangee (RG12). Le contrat impose d'ailleurs {@code 409 relecture deja
     * rendue} sur cette operation, ce qui decrit precisement une seconde tentative.</p>
     *
     * <p><b>Apres la cloture, le rendu reste possible</b> (RG21) : la clôture ferme
     * les depots et les presences, pas les notes des relectures deja assignees.</p>
     *
     * @throws RessourceIntrouvableException 404 {@code RELECTURE_INTROUVABLE}
     * @throws ErreurMetier 403 {@code ACCES_REFUSE} si l'appelant n'est pas le relecteur designe
     * @throws ErreurMetier 400 {@code NOTE_INVALIDE} si la note est absente, decimale, hors 0-20
     * @throws ErreurMetier 409 {@code RELECTURE_DEJA_RENDUE} si la note a deja ete envoyee
     */
    @Transactional
    public RelectureRendue rendreRelecture(Long relectureId, Long etudiantAppelant,
                                           DemandeRelecture demande) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);

        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        CodeErreur.RELECTURE_INTROUVABLE,
                        "La relecture demandee est inconnue."));

        if (Objects.equals(relecture.getExercice().getEtudiantId(), etudiantAppelant)) {
            // Q5 : l'auteur ne peut jamais noter son propre exercice — le contrat
            // distingue ce cas de celui d'un simple etranger.
            throw new ErreurMetier(
                    CodeErreur.AUTO_EVALUATION_INTERDITE,
                    HttpStatus.FORBIDDEN,
                    "Un etudiant ne peut pas relire son propre exercice.");
        }
        if (!Objects.equals(relecture.getRelecteurId(), etudiantAppelant)) {
            throw new ErreurMetier(
                    CodeErreur.ACCES_REFUSE,
                    HttpStatus.FORBIDDEN,
                    "Seul le relecteur designe peut rendre cette relecture.");
        }

        // Q9 : la note est un entier de 0 a 20. La forme est verifiee ici et non par
        // une annotation seule : le contrat impose le code NOTE_INVALIDE (et non
        // CHAMP_MANQUANT) pour toute note absente, decimale ou hors bornes.
        Integer note = noteEntiere(demande.note());
        if (note == null || note < 0 || note > 20) {
            throw new ErreurMetier(
                    CodeErreur.NOTE_INVALIDE,
                    HttpStatus.BAD_REQUEST,
                    "La note doit etre un nombre entier compris entre 0 et 20.");
        }

        // Le contrat borne le commentaire a 2000 caracteres (schema DemandeRelecture) :
        // refus explicite plutot que de laisser la contrainte SQL repondre un 500.
        String commentaireBrut = demande.commentaire();
        if (commentaireBrut != null && commentaireBrut.trim().length() > 2000) {
            throw new ErreurMetier(
                    CodeErreur.VALEUR_INVALIDE,
                    HttpStatus.BAD_REQUEST,
                    "Le commentaire ne doit pas depasser 2000 caracteres.");
        }

        // Q15 / RG12 : l'etat est verifie en dernier, juste avant l'ecriture. La note
        // d'origine reste donc strictement inchangee derriere un 409.
        if (relecture.getStatut() == StatutRelecture.RENDUE) {
            throw new ErreurMetier(
                    CodeErreur.RELECTURE_DEJA_RENDUE,
                    HttpStatus.CONFLICT,
                    "Cette relecture a deja ete rendue et n'est plus modifiable.");
        }

        // Le commentaire est facultatif : vide ou absent, il n'est pas stocke.
        String commentaire = commentaireBrut == null ? null : commentaireBrut.trim();
        relecture.rendre(note, commentaire == null || commentaire.isEmpty() ? null : commentaire, maintenant);

        // RG12 : la relecture et l'exercice changent d'etat dans la meme transaction.
        Exercice exercice = relecture.getExercice();
        exercice.marquerRelu(maintenant);
        exerciceRepository.save(exercice);

        return RelectureRendue.depuis(relecture);
    }

    /**
     * Deux refus possibles derriere le meme statut {@code 409}, et le contrat les
     * distingue : une relecture en attente est un relecteur deja designe, une relecture
     * rendue est une note deja envoyee — donc definitive (Q15, RG12).
     */
    /**
     * Convertit la note recue en entier strict, ou renvoie {@code null} si elle est
     * absente ou decimale : c'est le service, et non la liaison JSON, qui tranche entre
     * {@code NOTE_INVALIDE} (note absente, decimale ou hors bornes, cf. contrat) et les
     * autres erreurs de forme.
     */
    private Integer noteEntiere(Number noteRecue) {
        if (noteRecue == null) {
            return null;
        }
        long valeur = noteRecue.longValue();
        if (valeur != noteRecue.doubleValue()) {
            return null; // note decimale (ex. 14.5)
        }
        return (int) valeur;
    }

    private ErreurMetier dejaAssignee(Relecture existante) {
        if (existante.getStatut() == StatutRelecture.RENDUE) {
            return new ErreurMetier(
                    CodeErreur.RELECTURE_DEJA_RENDUE,
                    HttpStatus.CONFLICT,
                    "La note de cet exercice a deja ete envoyee : elle est definitive.");
        }
        return new ErreurMetier(
                CodeErreur.RELECTURE_DEJA_ASSIGNEE,
                HttpStatus.CONFLICT,
                "Cet exercice a deja un relecteur designe.");
    }
}
