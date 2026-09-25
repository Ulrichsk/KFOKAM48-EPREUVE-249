import { appelApi } from './client'
import type { RelectureResume, RelectureDetail, RelectureRendue, DemandeRelecture } from '../types'

/**
 * Fonctions d'appel API des relectures.
 *
 * Le relecteur est designe par le header `X-Etudiant-Id` (identite declarative, Q1) :
 * chaque fonction de ce fichier est la seule a connaitre ce detail de transport.
 */

/** `GET /api/relectures` — les relectures confiees a l'etudiant (EF9). */
export function listerMesRelectures(etudiantId: number): Promise<RelectureResume[]> {
  return appelApi<RelectureResume[]>('/api/relectures', {
    headers: { 'X-Etudiant-Id': String(etudiantId) },
  })
}

/** `GET /api/relectures/{id}` — ouvre le lien a relire, premiere consultation horodatee (Q13). */
export function consulterRelecture(relectureId: number, etudiantId: number): Promise<RelectureDetail> {
  return appelApi<RelectureDetail>(`/api/relectures/${relectureId}`, {
    headers: { 'X-Etudiant-Id': String(etudiantId) },
  })
}

/** `POST /api/relectures/{id}` — rend la note et le commentaire, definitifs (EF10, Q15). */
export function rendreRelecture(
  relectureId: number,
  etudiantId: number,
  demande: DemandeRelecture,
): Promise<RelectureRendue> {
  return appelApi<RelectureRendue>(`/api/relectures/${relectureId}`, {
    method: 'POST',
    headers: { 'X-Etudiant-Id': String(etudiantId) },
    body: JSON.stringify(demande),
  })
}
