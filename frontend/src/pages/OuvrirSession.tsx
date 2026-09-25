import { useState, type FormEvent } from 'react'
import { ouvrirSession } from '../api/sessions'
import { ErreurApi } from '../api/client'
import type { SessionCreee } from '../types'

/**
 * Ecran Formateur : ouvrir une session et afficher le code a dicter (EF1).
 *
 * Les dates affichees viennent telles quelles de l'API : aucune regle metier
 * (duree de validite, expiration) n'est recalculee cote front. L'interface se
 * contente de presenter ce que le serveur a decide.
 */
export function OuvrirSession() {
  const [titre, setTitre] = useState('')
  const [promotionId, setPromotionId] = useState(1)
  const [session, setSession] = useState<SessionCreee | null>(null)
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault()
    setEnvoi(true)
    setErreur(null)
    try {
      setSession(await ouvrirSession({ titre, promotionId }))
    } catch (e) {
      setSession(null)
      setErreur(
        e instanceof ErreurApi
          ? `[${e.code}] ${e.message}`
          : 'Une erreur inattendue est survenue.',
      )
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="carte">
      <h2>Ouvrir une session</h2>
      <p className="aide">
        Le code genere expire 15 minutes apres l&apos;ouverture (Q2). Il est destine a etre
        dicte a voix haute.
      </p>

      <form onSubmit={soumettre} className="formulaire">
        <label className="champ">
          Titre de la seance
          <input
            type="text"
            value={titre}
            maxLength={160}
            placeholder="Algorithmique — seance 4"
            onChange={(evenement) => setTitre(evenement.target.value)}
            required
          />
        </label>

        <label className="champ">
          Promotion
          <input
            type="number"
            min={1}
            value={promotionId}
            onChange={(evenement) => setPromotionId(Number(evenement.target.value))}
          />
        </label>

        <button type="submit" disabled={envoi}>
          {envoi ? 'Ouverture…' : 'Ouvrir la session'}
        </button>
      </form>

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {session !== null && (
        <div className="resultat" aria-live="polite">
          <p>Session #{session.id} ouverte. Code a dicter :</p>
          <p className="code">{session.code}</p>
          <dl className="details">
            <dt>Ouverture</dt>
            <dd>{formater(session.ouvertureAt)}</dd>
            <dt>Expiration du code</dt>
            <dd>{formater(session.expirationAt)}</dd>
          </dl>
        </div>
      )}
    </section>
  )
}

/** Presentation seule : la valeur affichee est celle renvoyee par l'API. */
function formater(instantIso: string): string {
  return new Date(instantIso).toLocaleString('fr-FR')
}
