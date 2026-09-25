/**
 * Types partages du frontend, alignes sur `api/contrat.yaml`.
 *
 * Toute evolution du contrat se repercute ici : le typage strict fait echouer la
 * compilation des qu'un champ change, ce qui evite les divergences silencieuses.
 */

/** Format d'erreur impose par le backend, identique pour toutes les erreurs. */
export interface ReponseErreur {
  code: string
  message: string
}

/** Schema `Etudiant` du contrat. */
export interface Etudiant {
  id: number
  nom: string
  prenom: string
  promotionId: number
}
