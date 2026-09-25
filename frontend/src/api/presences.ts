import { appelApi } from './client'
import type { DemandePresence, PresenceCreee } from '../types'

/**
 * Fonctions d'appel API des presences.
 *
 * L'ecran Etudiant n'a pas a connaitre les codes d'erreur du contrat : il recoit
 * une `ErreurApi` portant `CODE_EXPIRE`, `DEJA_PRESENT`, `CODE_INCONNU`,
 * `SESSION_CLOTUREE` ou `ACCES_REFUSE`, et affiche le message du serveur tel quel.
 */

/** `POST /api/presences` — marque la presence avec le code de la session (EF2). */
export function marquerPresence(demande: DemandePresence): Promise<PresenceCreee> {
  return appelApi<PresenceCreee>('/api/presences', {
    method: 'POST',
    body: JSON.stringify(demande),
  })
}
