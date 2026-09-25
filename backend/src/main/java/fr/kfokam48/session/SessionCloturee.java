package fr.kfokam48.session;

import java.time.LocalDateTime;

/**
 * Reponse de {@code POST /api/sessions/{id}/cloture} (schema {@code SessionCloturee} du
 * contrat) : {@code { id, clotureAt }}. Aucune entite JPA exposee (ENF3).
 *
 * <p>La clôture étant idempotente, {@code clotureAt} vaut toujours la date de la
 * <b>premiere</b> clôture : repondre une seconde date creerait une ambiguite sur
 * l'instant ou les depots ont ete fermes.</p>
 */
public record SessionCloturee(Long id, LocalDateTime clotureAt) {

    public static SessionCloturee depuis(Session session) {
        return new SessionCloturee(session.getId(), session.getClotureAt());
    }
}
