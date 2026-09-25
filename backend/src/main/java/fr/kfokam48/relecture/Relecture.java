package fr.kfokam48.relecture;

import fr.kfokam48.exercice.Exercice;
import fr.kfokam48.referentiel.Etudiant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Relecture d'un exercice par un pair.
 *
 * <p>« Un seul relecteur par exercice » (Q6, RG7) n'est pas une convention applicative :
 * la colonne {@code exercice_id} est unique, et l'association est donc modelisee en
 * {@link OneToOne} — ce qui traduit exactement la regle.</p>
 *
 * <p>Les colonnes {@code note}, {@code commentaire} et {@code rendu_at} restent nulles
 * tant que la relecture n'est pas rendue : la base ne peut donc pas contenir une note
 * « en attente », ni une note rendue sans horodatage. Aucune colonne ne permet de
 * reecrire une note apres envoi, ce qui est volontaire (Q15, decision 7.1).</p>
 *
 * <p>Les operations de consultation du lien (issue 09) et de rendu de note (issue 10)
 * seront ajoutees ici : elles appartiennent au cycle de vie de cette entite.</p>
 */
@Entity
@Table(name = "relectures")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    @ManyToOne
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutRelecture statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigne_par", nullable = false, length = 10)
    private AssignePar assignePar;

    @Column(name = "assigne_at", nullable = false)
    private LocalDateTime assigneAt;

    @Column(name = "lien_consulte_at")
    private LocalDateTime lienConsulteAt;

    @Column(name = "note")
    private Integer note;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    @Column(name = "rendu_at")
    private LocalDateTime renduAt;

    protected Relecture() {
        // requis par JPA
    }

    public Relecture(Exercice exercice, Etudiant relecteur, AssignePar assignePar, LocalDateTime maintenant) {
        this.exercice = exercice;
        this.relecteur = relecteur;
        this.assignePar = assignePar;
        this.assigneAt = maintenant;
        this.statut = StatutRelecture.EN_ATTENTE;
    }

    /**
     * Premiere consultation du lien par le relecteur (Q13, RG16) : horodatage fige.
     *
     * <p>Le service ne l'appelle que si {@code lienConsulteAt} est encore nul : la
     * premiere consultation fait foi, les suivantes ne reecrivent jamais la date.
     * Tant que cette colonne est nulle, l'auteur peut encore remplacer son lien.</p>
     */
    public void marquerLienConsulte(LocalDateTime instant) {
        this.lienConsulteAt = instant;
    }

    /**
     * Envoie la note : etat terminal, aucune reecriture possible (Q15, RG12).
     *
     * <p>Le service ne l'appelle qu'apres avoir verifie que la relecture est encore en
     * attente : il n'existe donc aucun chemin qui reecrive une note rendue, et la base
     * ne contient jamais une note rendue sans horodatage. Aucune methode de correction
     * n'existe volontairement (decision 7.1 : Q15 prime sur Q10).</p>
     */
    public void rendre(int note, String commentaire, LocalDateTime instantRendu) {
        this.note = note;
        this.commentaire = commentaire;
        this.renduAt = instantRendu;
        this.statut = StatutRelecture.RENDUE;
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Long getExerciceId() {
        return exercice == null ? null : exercice.getId();
    }

    public Etudiant getRelecteur() {
        return relecteur;
    }

    public Long getRelecteurId() {
        return relecteur == null ? null : relecteur.getId();
    }

    public StatutRelecture getStatut() {
        return statut;
    }

    public AssignePar getAssignePar() {
        return assignePar;
    }

    public LocalDateTime getAssigneAt() {
        return assigneAt;
    }

    public LocalDateTime getLienConsulteAt() {
        return lienConsulteAt;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getRenduAt() {
        return renduAt;
    }
}
