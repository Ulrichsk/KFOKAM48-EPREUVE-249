package fr.kfokam48.presence;

/**
 * Reponse de {@code POST /api/presences} :
 * {@code { id, sessionId, etudiantId, source }}.
 *
 * <p>Exactement les quatre champs du contrat : ni identite de l'etudiant, ni code
 * de session, ni entite JPA (ENF3).</p>
 */
public record PresenceCreee(Long id, Long sessionId, Long etudiantId, SourcePresence source) {

    public static PresenceCreee depuis(Presence presence) {
        return new PresenceCreee(
                presence.getId(),
                presence.getSessionId(),
                presence.getEtudiantId(),
                presence.getSource());
    }
}
