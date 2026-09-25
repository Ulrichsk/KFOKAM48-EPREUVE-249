package fr.kfokam48.presence;

import fr.kfokam48.referentiel.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Acces aux presences : aucune logique metier ici. */
public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /**
     * Etudiants eligibles au tirage au sort du relecteur (RG8, RG6) : presents a la
     * session, quelle que soit la source de la presence, a l'exclusion de l'auteur.
     *
     * <p>Une seule requete, sans parcours des presences cote application. La source de la
     * presence n'est volontairement pas filtree : un etudiant ajoute a la main par le
     * formateur (Q14) doit pouvoir relire (decision 7.2 du cahier des charges).</p>
     */
    @Query("""
            select p.etudiant
            from Presence p
            where p.session.id = :sessionId
              and p.etudiant.id <> :auteurId
            """)
    List<Etudiant> findEligiblesPourTirage(@Param("sessionId") Long sessionId,
                                           @Param("auteurId") Long auteurId);

    /**
     * Un etudiant n'a qu'une presence par session (RG4). La requete sert a
     * produire le message {@code DEJA_PRESENT} ; la contrainte d'unicite de la
     * table reste la garante de l'invariant (cf. {@link Presence}).
     */
    boolean existsBySession_IdAndEtudiant_Id(Long sessionId, Long etudiantId);

    /** Nombre de presences d'une session, utilise par les tests et le tableau. */
    long countBySession_Id(Long sessionId);
}
