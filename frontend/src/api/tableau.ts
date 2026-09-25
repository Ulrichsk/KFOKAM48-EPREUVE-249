import { appelApi } from './client'
import type { LigneTableau } from '../types'

/**
 * Fonctions d'appel API du tableau de bord.
 *
 * La moyenne est calculee par l'API (RG18) : l'ecran ne fait que l'afficher,
 * il ne la recalcule jamais a partir des notes — aucune regle metier dupliquee.
 */

/** `GET /api/tableau?promotionId=` — une ligne par etudiant de la promotion (Q16). */
export function obtenirTableau(promotionId: number): Promise<LigneTableau[]> {
  return appelApi<LigneTableau[]>(`/api/tableau?promotionId=${promotionId}`)
}
