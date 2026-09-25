package fr.kfokam48.referentiel;

/**
 * Etudiant expose par l'API (schema {@code Etudiant} de api/contrat.yaml).
 *
 * <p>DTO volontairement minimal : ni {@code createdAt}, ni l'entite
 * {@link Promotion} imbriquee. L'entite JPA n'est jamais serialisee (ENF3).</p>
 */
public record EtudiantDto(Long id, String nom, String prenom, Long promotionId) {

    public static EtudiantDto depuis(Etudiant etudiant) {
        return new EtudiantDto(
                etudiant.getId(),
                etudiant.getNom(),
                etudiant.getPrenom(),
                etudiant.getPromotionId());
    }
}
