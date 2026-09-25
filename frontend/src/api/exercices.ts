import { appelApi } from './client'
import type { DemandeDepotExercice, ExerciceCree } from '../types'

/**
 * Fonctions d'appel API des exercices.
 *
 * Les regles de recevabilite d'un lien (URL absolue http/https, 500 caracteres) sont
 * verifiees par le serveur : l'ecran se contente d'afficher le message renvoye pour
 * `LIEN_INVALIDE`, sans dupliquer la regle cote client.
 */

/** `POST /api/exercices` — depose le lien de l'exercice (EF5). */
export function deposerExercice(demande: DemandeDepotExercice): Promise<ExerciceCree> {
  return appelApi<ExerciceCree>('/api/exercices', {
    method: 'POST',
    body: JSON.stringify(demande),
  })
}
