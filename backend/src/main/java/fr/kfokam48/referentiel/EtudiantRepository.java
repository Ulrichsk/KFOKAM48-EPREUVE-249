package fr.kfokam48.referentiel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acces en lecture aux etudiants : aucune logique metier ici. */
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    /**
     * Etudiants d'une promotion, tries par nom puis prenom (EF15 : la liste de
     * selection de Q1 doit etre lisible). Le tri ne depend donc pas de la
     * collation du SGBD, ce qui garantit le meme ordre sur PostgreSQL et sur H2.
     */
    List<Etudiant> findByPromotion_IdOrderByNomAscPrenomAsc(Long promotionId);
}
