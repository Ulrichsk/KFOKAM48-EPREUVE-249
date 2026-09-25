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

/** Corps de `POST /api/sessions`. */
export interface DemandeOuvertureSession {
  titre: string
  promotionId: number
}

/** Schema `SessionCreee` du contrat : le code est a dicter aux etudiants. */
export interface SessionCreee {
  id: number
  code: string
  ouvertureAt: string
  expirationAt: string
}

/** Corps de `POST /api/presences`. */
export interface DemandePresence {
  code: string
  etudiantId: number
}

/** Schema `PresenceCreee` du contrat. */
export interface PresenceCreee {
  id: number
  sessionId: number
  etudiantId: number
  source: 'ETUDIANT' | 'FORMATEUR'
}
