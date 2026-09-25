package fr.kfokam48.session;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Reponse de {@code POST /api/sessions} : {@code { id, code, ouvertureAt, expirationAt }}.
 *
 * <p>Les instants sont exposes en UTC au format ISO-8601 (ENF6), alors que l'entite
 * les stocke en {@code LocalDateTime} UTC : la conversion est faite ici, une fois,
 * pour que le contrat soit respecte partout de la meme facon.</p>
 */
public record SessionCreee(Long id, String code, Instant ouvertureAt, Instant expirationAt) {

    public static SessionCreee depuis(Session session) {
        return new SessionCreee(
                session.getId(),
                session.getCode(),
                session.getOuvertureAt().toInstant(ZoneOffset.UTC),
                session.getExpirationAt().toInstant(ZoneOffset.UTC));
    }
}
