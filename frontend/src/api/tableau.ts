import { appelApi } from './client'
import type { LigneTableau } from '../types'
import type { DemandeAssignationRelecteur, RelectureAssignee } from '../types'

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

/**
 * `POST /api/exercices/{id}/relecteur` — designation manuelle d'un relecteur (EF8, Q11),
 * mise en oeuvre depuis l'ecran du tableau par le changement de besoin (issue 16).
 * L'API tranche les regles : 403 si l'auteur est propose ou si l'etudiant est hors
 * promotion, 409 si un relecteur existe deja — ses messages sont affiches tels quels.
 */
export function designerRelecteur(
  exerciceId: number,
  demande: DemandeAssignationRelecteur,
): Promise<RelectureAssignee> {
  return appelApi<RelectureAssignee>(`/api/exercices/${exerciceId}/relecteur`, {
    method: 'POST',
    body: JSON.stringify(demande),
  })
}
