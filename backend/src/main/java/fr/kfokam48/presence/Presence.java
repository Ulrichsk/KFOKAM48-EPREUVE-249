package fr.kfokam48.presence;

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
 * Presence d'un etudiant a une session.
 *
 * <p>L'unicite « un etudiant, une presence par session » (RG4) n'est pas une
 * convention applicative : elle est portee par la contrainte
 * {@code uq_presences_session_etudiant} creee par la migration V1. Le service
 * verifie l'existence avant d'inserer pour renvoyer un message clair, mais c'est
 * la base qui rend le doublon impossible.</p>
 */
@Entity
@Table(name = "presences")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private SourcePresence source;

    @Column(name = "ajoutee_at", nullable = false)
    private LocalDateTime ajouteeAt;

    protected Presence() {
        // requis par JPA
    }

    public Presence(Session session, Etudiant etudiant, SourcePresence source, LocalDateTime ajouteeAt) {
        this.session = session;
        this.etudiant = etudiant;
        this.source = source;
        this.ajouteeAt = ajouteeAt;
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

    public SourcePresence getSource() {
        return source;
    }

    public LocalDateTime getAjouteeAt() {
        return ajouteeAt;
    }
}
