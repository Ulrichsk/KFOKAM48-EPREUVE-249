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

/**
 * Cycle de vie d'un exercice, tel qu'expose par le contrat.
 * `DEPOSE` existe en base mais n'est jamais renvoye au client : c'est un etat
 * transitoire interne a la transaction de depot.
 */
export type StatutExercice = 'EN_ATTENTE' | 'SANS_RELECTEUR' | 'RELU'

/** Corps de `POST /api/exercices`. */
export interface DemandeDepotExercice {
  sessionId: number
  etudiantId: number
  lien: string
}

/** Schema `ExerciceCree` du contrat. */
export interface ExerciceCree {
  id: number
  statut: StatutExercice
}

/** Schema `LigneTableau` du contrat : une ligne par etudiant de la promotion (Q16). */
export interface LigneTableau {
  etudiantId: number
  nom: string
  prenom: string
  presences: number
  presencesFormateur: number
  exercicesDeposes: number
  /** Moyenne des notes recues, calculee par l'API (RG18) — jamais par le client. */
  moyenne: number
  relecturesEnAttente: number
}
