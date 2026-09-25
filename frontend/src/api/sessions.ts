import { appelApi } from './client'
import type { DemandeOuvertureSession, SessionCreee } from '../types'

/**
 * Fonctions d'appel API des sessions de cours.
 *
 * Le composant qui ouvre une session n'a pas besoin de connaitre le chemin, le
 * verbe ni le format de transport : il appelle `ouvrirSession` et recoit un
 * `SessionCreee`, ou une `ErreurApi` portant le code du contrat.
 */

/** `POST /api/sessions` — ouvre une session et renvoie son code (EF1). */
export function ouvrirSession(demande: DemandeOuvertureSession): Promise<SessionCreee> {
  return appelApi<SessionCreee>('/api/sessions', {
    method: 'POST',
    body: JSON.stringify(demande),
  })
}
