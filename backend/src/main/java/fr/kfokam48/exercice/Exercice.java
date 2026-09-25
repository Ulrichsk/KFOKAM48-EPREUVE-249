package fr.kfokam48.exercice;

import fr.kfokam48.referentiel.Etudiant;
import fr.kfokam48.session.Session;
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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Exercice depose par un etudiant pour une session (Q12, Q13).
 *
 * <p>« Un exercice par etudiant et par session » n'est pas une convention applicative :
 * la contrainte {@code uq_exercices_session_etudiant} de la migration V1 le garantit. Le
 * service verifie l'existence pour renvoyer un message clair, la base restant la
 * garante de l'invariant.</p>
 */
@Entity
@Table(name = "exercices")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(name = "lien", nullable = false, length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutExercice statut;

    @Column(name = "depose_at", nullable = false)
    private LocalDateTime deposeAt;

    @Column(name = "maj_at", nullable = false)
    private LocalDateTime majAt;

    protected Exercice() {
        // requis par JPA
    }

    public Exercice(Session session, Etudiant etudiant, String lien, StatutExercice statut,
                    LocalDateTime maintenant) {
        this.session = session;
        this.etudiant = etudiant;
        this.lien = lien;
        this.statut = statut;
        this.deposeAt = maintenant;
        this.majAt = maintenant;
    }

    /** Remplacement du lien tant que la relecture n'a pas commence (Q13, RG16). */
    public void remplacerLien(String nouveauLien, LocalDateTime maintenant) {
        this.lien = nouveauLien;
        this.majAt = maintenant;
    }

    /**
     * Le tirage au sort a trouve un relecteur : l'exercice attend sa note (Q7, RG9).
     *
     * <p>Le statut passe directement de {@code DEPOSE} a {@code EN_ATTENTE} : {@code DEPOSE}
     * n'existe que le temps de la transaction de depot et n'est jamais expose (D4).</p>
     */
    public void confierAUnRelecteur(LocalDateTime maintenant) {
        this.statut = StatutExercice.EN_ATTENTE;
        this.majAt = maintenant;
    }

    /**
     * Aucun etudiant eligible : l'exercice reste sans relecteur et doit rester visible
     * dans le tableau du formateur, qui pourra en designer un (Q11, RG10).
     */
    public void marquerSansRelecteur(LocalDateTime maintenant) {
        this.statut = StatutExercice.SANS_RELECTEUR;
        this.majAt = maintenant;
    }

    public Long getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public Long getSessionId() {
        return session == null ? null : session.getId();
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public Long getEtudiantId() {
        return etudiant == null ? null : etudiant.getId();
    }

    public String getLien() {
        return lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public LocalDateTime getDeposeAt() {
        return deposeAt;
    }

    public LocalDateTime getMajAt() {
        return majAt;
    }
}
