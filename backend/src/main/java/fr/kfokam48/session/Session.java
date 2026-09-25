package fr.kfokam48.session;

import fr.kfokam48.referentiel.Promotion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Session de cours ouverte par le formateur.
 *
 * <p>Deux jalons distincts bornent sa vie (decision 7.5 du cahier des charges) :</p>
 * <ul>
 *   <li>{@code expirationAt} : fin de validite du code de presence (Q2, RG2) ;</li>
 *   <li>{@code clotureAt} : clôture explicite par le formateur, qui ferme les depots
 *       (Q12, RG15) et les presences (RG21).</li>
 * </ul>
 *
 * <p>L'expiration du code n'empeche donc pas un depot : les deux dates ne sont pas
 * interchangeables.</p>
 */
@Entity
@Table(name = "sessions")
public class Session {

    /** Duree de validite du code de presence, imposee par Q2. */
    public static final Duration DUREE_VALIDITE_CODE = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "titre", nullable = false, length = 160)
    private String titre;

    /** Code dicte aux etudiants (RG1), unique en base par contrainte. */
    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private LocalDateTime ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private LocalDateTime expirationAt;

    @Column(name = "cloture_at")
    private LocalDateTime clotureAt;

    protected Session() {
        // requis par JPA
    }

    /**
     * Ouvre une session : l'expiration du code est deduite de l'ouverture (RG2),
     * ce qui interdit de creer une session dont le code serait deja mort.
     */
    public Session(Promotion promotion, String titre, String code, LocalDateTime ouvertureAt) {
        this.promotion = promotion;
        this.titre = titre;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = ouvertureAt.plus(DUREE_VALIDITE_CODE);
        this.clotureAt = null;
    }

    /** Vrai si le code a depasse sa duree de validite (RG2, RG3). */
    public boolean codeExpireA(LocalDateTime instant) {
        return instant.isAfter(expirationAt);
    }

    /** Vrai si le formateur a clos la session (RG21). */
    public boolean estCloturee() {
        return clotureAt != null;
    }

    public Long getId() {
        return id;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public Long getPromotionId() {
        return promotion == null ? null : promotion.getId();
    }

    public String getTitre() {
        return titre;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getOuvertureAt() {
        return ouvertureAt;
    }

    public LocalDateTime getExpirationAt() {
        return expirationAt;
    }

    public LocalDateTime getClotureAt() {
        return clotureAt;
    }
}
