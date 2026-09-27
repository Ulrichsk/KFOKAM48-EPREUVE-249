package fr.kfokam48.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès aux traces d'assignation manuelle (RG23).
 *
 * <p>Écriture seule côté production : les traces sont posées par
 * {@code RelectureService} dans la transaction de l'assignation. Aucune lecture
 * métier n'en dépend — l'exposition éventuelle au formateur est laissée à une
 * évolution ultérieure, ce fichier de migration est le seul engagement pris.
 * Le finder par exercice sert les tests d'intégration, qui vérifient qu'une
 * assignation réussie laisse bien une trace, et qu'un refus n'en laisse aucune.</p>
 */
public interface AuditAssignationManuelleRepository extends JpaRepository<AuditAssignationManuelle, Long> {

    Optional<AuditAssignationManuelle> findByExercice_Id(Long exerciceId);
}
