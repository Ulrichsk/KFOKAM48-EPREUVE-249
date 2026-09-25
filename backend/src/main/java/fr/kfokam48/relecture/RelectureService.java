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
     * Deux refus possibles derriere le meme statut {@code 409}, et le contrat les
     * distingue : une relecture en attente est un relecteur deja designe, une relecture
     * rendue est une note deja envoyee — donc definitive (Q15, RG12).
     */
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
