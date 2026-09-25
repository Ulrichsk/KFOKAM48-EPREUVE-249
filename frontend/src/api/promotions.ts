import { appelApi } from './client'
import type { Etudiant } from '../types'

/**
 * Fonctions d'appel API du referentiel.
 *
 * Les composants n'utilisent que ces fonctions : ils ne connaissent ni les
 * chemins du contrat, ni le format de transport.
 */

/** `GET /api/promotions/{id}/etudiants` — liste de noms de Q1 (EF15). */
export function listerEtudiants(promotionId: number): Promise<Etudiant[]> {
  return appelApi<Etudiant[]>(`/api/promotions/${promotionId}/etudiants`)
}
