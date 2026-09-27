package fr.kfokam48.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux traces d'assignation manuelle (RG23).
 *
 * <p>Écriture seule aujourd'hui : les traces sont posées par
 * {@code RelectureService} dans la transaction de l'assignation. Aucune lecture
 * métier n'en dépend — l'exposition éventuelle au formateur est laissée à une
 * évolution ultérieure, ce fichier de migration est le seul engagement pris.</p>
 */
public interface AuditAssignationManuelleRepository extends JpaRepository<AuditAssignationManuelle, Long> {
}
