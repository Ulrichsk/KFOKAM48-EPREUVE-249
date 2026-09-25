package fr.kfokam48.referentiel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acces en lecture aux promotions : aucune logique metier ici. */
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
