package fr.kfokam48.relecture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acces aux relectures : aucune logique metier ici. */
public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** Un exercice n'a qu'un relecteur : la contrainte d'unicite en est la garante (Q6). */
    Optional<Relecture> findByExercice_Id(Long exerciceId);

    /** Nombre de relectures d'un exercice, utilise par les tests (doit valoir 0 ou 1). */
    long countByExercice_Id(Long exerciceId);
}
