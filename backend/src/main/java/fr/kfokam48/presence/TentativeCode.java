package fr.kfokam48.presence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Compteur d'echecs de saisie du code, un par etudiant (Q4, RG5, hypothese H7).
 *
 * <p>Q4 dit « bloquez-<i>le</i> deux minutes » : le compteur est donc porte par
 * l'etudiant, toutes sessions confondues, et non par couple (etudiant, session).</p>
 *
 * <p>L'association vers {@code etudiants} est portee par la cle etrangere du schema
 * (migration V1) mais volontairement <b>non mappee</b> comme relation JPA : le
 * compteur est lu et ecrit depuis une transaction separee
 * ({@link CompteurTentativesCode}), et partager une meme instance d'entite entre
 * deux contextes de persistance est precisement ce qu'il faut eviter ici.</p>
 *
 * <p>La logique de blocage ne vit pas dans cette classe : elle est dans
 * {@link EtatTentativesCode}, qui est testable sans base.</p>
 */
@Entity
@Table(name = "tentatives_code")
public class TentativeCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etudiant_id", nullable = false, unique = true)
    private Long etudiantId;

    @Column(name = "nb_echecs", nullable = false)
    private int nbEchecs;

    @Column(name = "bloque_jusqu_a")
    private LocalDateTime bloqueJusquA;

    @Column(name = "maj_at", nullable = false)
    private LocalDateTime majAt;

    protected TentativeCode() {
        // requis par JPA
    }

    public TentativeCode(Long etudiantId, LocalDateTime maintenant) {
        this.etudiantId = etudiantId;
        this.nbEchecs = 0;
        this.majAt = maintenant;
    }

    /** Etat courant, sous la forme que manipule la politique {@link EtatTentativesCode}. */
    public EtatTentativesCode etat() {
        return new EtatTentativesCode(nbEchecs, bloqueJusquA);
    }

    /** Applique un nouvel etat issu de la politique de blocage. */
    public void appliquer(EtatTentativesCode etat, LocalDateTime maintenant) {
        this.nbEchecs = etat.nbEchecs();
        this.bloqueJusquA = etat.bloqueJusquA();
        this.majAt = maintenant;
    }

    public Long getId() {
        return id;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public int getNbEchecs() {
        return nbEchecs;
    }

    public LocalDateTime getBloqueJusquA() {
        return bloqueJusquA;
    }

    public LocalDateTime getMajAt() {
        return majAt;
    }
}
