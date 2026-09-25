package fr.kfokam48.referentiel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Promotion : groupe d'etudiants anime par le formateur (Q16).
 *
 * <p>Entite JPA strictement interne : elle n'est jamais serialisee en JSON
 * (ENF3). La couche HTTP ne connait que les DTO.</p>
 */
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 120)
    private String nom;

    /** Horodatage UTC (ENF6). */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Promotion() {
        // requis par JPA
    }

    public Promotion(String nom, LocalDateTime createdAt) {
        this.nom = nom;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
