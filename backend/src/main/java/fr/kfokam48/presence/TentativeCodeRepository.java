package fr.kfokam48.presence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acces au compteur de tentatives : aucune logique metier ici. */
public interface TentativeCodeRepository extends JpaRepository<TentativeCode, Long> {

    /**
     * Le compteur est unique par etudiant (contrainte {@code uq_tentatives_code_etudiant}).
     *
     * <p>Le nom suit le champ {@code etudiantId} de l'entite et non une association :
     * {@link TentativeCode} mappe une colonne simple, il n'y a donc pas de propriete
     * {@code etudiant} a traverser (contrairement a {@code Presence}).</p>
     */
    Optional<TentativeCode> findByEtudiantId(Long etudiantId);
}
