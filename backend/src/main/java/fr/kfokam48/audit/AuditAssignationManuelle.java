package fr.kfokam48.audit;

import fr.kfokam48.exercice.Exercice;
import fr.kfokam48.referentiel.Etudiant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Trace d'une assignation manuelle de relecteur (RG23, enveloppe étape 3).
 *
 * <p>Écrite à chaque désignation réussie par le formateur (voie {@code FORMATEUR}),
 * dans la même transaction que l'assignation : la trace existe si et seulement si
 * l'opération a réussi. La table {@code relectures} reste la source de vérité de
 * l'état ; celle-ci n'ajoute que le <b>qui</b> ({@code assigneParId}), absent de
 * toute autre table — l'identité du formateur n'existe pas (Q1, H1), c'est l'acte
 * qui est tracé.</p>
 *
 * <p>Le tirage automatique n'est pas tracé : il ne met en jeu aucun acte humain,
 * {@code assigne_par = SYSTEME} l'atteste déjà sur la relecture elle-même.</p>
 *
 * <p>Pure observation : aucune lecture de cette table n'influence un comportement
 * métier. Elle sert l'auditabilité du déblocage manuel, devenu d'usage courant
 * depuis l'écran du tableau (issue 16).</p>
 */
@Entity
@Table(name = "audits_assignation_manuelle")
public class AuditAssignationManuelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false)
    private Exercice exercice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    /** Étudiant au nom duquel l'acte a été posé (aucune authentification : Q1, H1). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigne_par_id", nullable = false)
    private Etudiant assignePar;

    @Column(name = "assigne_at", nullable = false)
    private LocalDateTime assigneAt;

    protected AuditAssignationManuelle() {
        // requis par JPA
    }

    public AuditAssignationManuelle(Exercice exercice, Etudiant relecteur, Etudiant assignePar,
                                    LocalDateTime assigneAt) {
        this.exercice = exercice;
        this.relecteur = relecteur;
        this.assignePar = assignePar;
        this.assigneAt = assigneAt;
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Etudiant getRelecteur() {
        return relecteur;
    }

    public Etudiant getAssignePar() {
        return assignePar;
    }

    public LocalDateTime getAssigneAt() {
        return assigneAt;
    }
}
