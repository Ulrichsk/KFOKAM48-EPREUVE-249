import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { listerEtudiants } from '../api/promotions'
import { marquerPresence } from '../api/presences'
import { ErreurApi } from '../api/client'
import type { Etudiant, PresenceCreee } from '../types'

/**
 * Ecran Etudiant : choisir son nom dans la liste (Q1), saisir le code dicte par
 * le formateur et marquer sa presence (EF2).
 *
 * L'ecran se contente de presenter : il n'anticipe aucune regle metier. C'est le
 * serveur qui decide si le code est valide, expire, deja utilise ou hors
 * promotion, et son message est affiche tel quel (Q2, Q3, Q4, RG19).
 */
export function MarquerPresence() {
  const [promotionId, setPromotionId] = useState(1)
  const [etudiants, setEtudiants] = useState<Etudiant[]>([])
  const [etudiantId, setEtudiantId] = useState('')
  const [code, setCode] = useState('')
  const [presence, setPresence] = useState<PresenceCreee | null>(null)
  const [chargementListe, setChargementListe] = useState(false)
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const chargerListe = useCallback(async (idPromotion: number) => {
    setChargementListe(true)
    setErreur(null)
    try {
      const liste = await listerEtudiants(idPromotion)
      setEtudiants(liste)
      setEtudiantId(liste.length > 0 ? String(liste[0].id) : '')
    } catch (e) {
      setEtudiants([])
      setEtudiantId('')
      setErreur(messageDe(e))
    } finally {
      setChargementListe(false)
    }
  }, [])

  useEffect(() => {
    void chargerListe(promotionId)
  }, [chargerListe, promotionId])

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault()
    setEnvoi(true)
    setErreur(null)
    try {
      setPresence(await marquerPresence({ code, etudiantId: Number(etudiantId) }))
    } catch (e) {
      setPresence(null)
      setErreur(messageDe(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="carte">
      <h2>Marquer ma presence</h2>
      <p className="aide">
        Le code est celui dicte par le formateur. Il ne fonctionne que 15 minutes apres
        l&apos;ouverture de la session, et une seule fois par etudiant.
      </p>

      <form onSubmit={soumettre} className="formulaire">
        <label className="champ">
          Promotion
          <input
            type="number"
            min={1}
            value={promotionId}
            onChange={(evenement) => setPromotionId(Number(evenement.target.value))}
          />
        </label>

        <label className="champ">
          Je suis
          <select
            value={etudiantId}
            onChange={(evenement) => setEtudiantId(evenement.target.value)}
            disabled={chargementListe || etudiants.length === 0}
            required
          >
            {chargementListe && <option value="">Chargement…</option>}
            {!chargementListe && etudiants.length === 0 && (
              <option value="">Aucun etudiant pour cette promotion</option>
            )}
            {etudiants.map((etudiant) => (
              <option key={etudiant.id} value={etudiant.id}>
                {etudiant.nom} {etudiant.prenom}
              </option>
            ))}
          </select>
        </label>

        <label className="champ">
          Code de presence
          <input
            type="text"
            className="code-saisie"
            value={code}
            maxLength={10}
            placeholder="K7M2QP"
            onChange={(evenement) => setCode(evenement.target.value)}
            required
          />
        </label>

        <button type="submit" disabled={envoi || etudiantId === ''}>
          {envoi ? 'Enregistrement…' : 'Marquer ma presence'}
        </button>
      </form>

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {presence !== null && (
        <div className="resultat" aria-live="polite">
          <p className="succes">
            Presence enregistree pour la session #{presence.sessionId} (source{' '}
            {presence.source}).
          </p>
        </div>
      )}
    </section>
  )
}

/** Traduit une erreur en message affichable, sans jamais inventer de texte. */
function messageDe(e: unknown): string {
  return e instanceof ErreurApi
    ? `[${e.code}] ${e.message}`
    : 'Une erreur inattendue est survenue.'
}
