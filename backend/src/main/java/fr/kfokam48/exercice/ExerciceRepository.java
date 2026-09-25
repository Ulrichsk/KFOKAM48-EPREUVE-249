package fr.kfokam48.exercice;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acces aux exercices : aucune logique metier ici. */
public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    /**
     * Un etudiant ne depose qu'un exercice par session. La contrainte d'unicite de la
     * table reste la garante de l'invariant ; cette requete sert a produire le message
     * {@code EXERCICE_DEJA_DEPOSE}.
     */
    boolean existsBySession_IdAndEtudiant_Id(Long sessionId, Long etudiantId);

    /** Nombre d'exercices d'une session, utilise par les tests et le tableau de bord. */
    long countBySession_Id(Long sessionId);
}
