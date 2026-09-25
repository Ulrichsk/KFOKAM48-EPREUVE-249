import type { ReponseErreur } from '../types'

/**
 * Couche d'appel API unique du frontend.
 *
 * Aucun composant n'appelle `fetch` directement : tout passe par `appelApi`, ce
 * qui garantit un seul endroit ou gerer l'URL de base, les en-tetes, la
 * deserialisation JSON et la traduction des erreurs du backend
 * (`{ code, message }`) en exception exploitable par l'interface.
 */

/** URL de base surchargeable par `VITE_API_URL` ; vide en dev grâce au proxy Vite. */
const BASE_URL: string = import.meta.env.VITE_API_URL ?? ''

/** Erreur API normalisee : porte le code du contrat, jamais de trace technique. */
export class ErreurApi extends Error {
  readonly code: string
  readonly statut: number

  constructor(code: string, message: string, statut: number) {
    super(message)
    this.name = 'ErreurApi'
    this.code = code
    this.statut = statut
  }
}

/**
 * Execute un appel HTTP et renvoie le corps deserialise.
 *
 * @throws ErreurApi si le serveur est injoignable, si la reponse est en erreur,
 *         ou si le corps d'erreur n'est pas au format attendu.
 */
export async function appelApi<T>(chemin: string, init: RequestInit = {}): Promise<T> {
  const entetes: Record<string, string> = {
    Accept: 'application/json',
    ...(init.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...((init.headers as Record<string, string> | undefined) ?? {}),
  }

  let reponse: Response
  try {
    reponse = await fetch(`${BASE_URL}${chemin}`, { ...init, headers: entetes })
  } catch {
    throw new ErreurApi(
      'RESEAU_INDISPONIBLE',
      "Le serveur est injoignable. Verifiez qu'il est bien demarre.",
      0,
    )
  }

  if (reponse.status === 204) {
    return undefined as T
  }

  if (!reponse.ok) {
    const corps = (await reponse.json().catch(() => null)) as ReponseErreur | null
    throw new ErreurApi(
      corps?.code ?? 'ERREUR_INCONNUE',
      corps?.message ?? 'Une erreur inattendue est survenue.',
      reponse.status,
    )
  }

  return (await reponse.json()) as T
}
