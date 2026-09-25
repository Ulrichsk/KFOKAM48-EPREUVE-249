package fr.kfokam48.referentiel;

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
 * Etudiant d'une promotion.
 *
 * <p>Q1 : aucune authentification. Le nom et le prenom servent uniquement a
 * alimenter la liste de selection ; il n'existe volontairement aucune colonne de
 * mot de passe (hypothese H1 du cahier des charges).</p>
 */
@Entity
@Table(name = "etudiants")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "nom", nullable = false, length = 80)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 80)
    private String prenom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Etudiant() {
        // requis par JPA
    }

    public Etudiant(Promotion promotion, String nom, String prenom, LocalDateTime createdAt) {
        this.promotion = promotion;
        this.nom = nom;
        this.prenom = prenom;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    /**
     * Identifiant de la promotion sans declencher le chargement de l'association :
     * accessible sur un proxy non initialise, ce qui evite toute requete
     * supplementaire lors du mapping en DTO.
     */
    public Long getPromotionId() {
        return promotion == null ? null : promotion.getId();
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
