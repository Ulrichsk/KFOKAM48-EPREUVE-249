package fr.kfokam48.relecture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acces aux relectures : aucune logique metier ici. */
public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** Un exercice n'a qu'un relecteur : la contrainte d'unicite en est la garante (Q6). */
    Optional<Relecture> findByExercice_Id(Long exerciceId);

    /** Nombre de relectures d'un exercice, utilise par les tests (doit valoir 0 ou 1). */
    long countByExercice_Id(Long exerciceId);

    /**
     * Relectures confiees a un etudiant (EF9), les plus recentes d'abord : l'ecran du
     * relecteur affiche d'abord ce qui attend encore sa note. Le tri explicite garantit
     * le meme ordre sur PostgreSQL et sur H2.
     */
    List<Relecture> findByRelecteur_IdOrderByAssigneAtDesc(Long relecteurId);
}
