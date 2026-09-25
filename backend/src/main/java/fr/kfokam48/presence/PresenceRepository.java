package fr.kfokam48.presence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acces aux presences : aucune logique metier ici. */
public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /**
     * Un etudiant n'a qu'une presence par session (RG4). La requete sert a
     * produire le message {@code DEJA_PRESENT} ; la contrainte d'unicite de la
     * table reste la garante de l'invariant (cf. {@link Presence}).
     */
    boolean existsBySession_IdAndEtudiant_Id(Long sessionId, Long etudiantId);

    /** Nombre de presences d'une session, utilise par les tests et le tableau. */
    long countBySession_Id(Long sessionId);
}
