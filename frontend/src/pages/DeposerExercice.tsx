import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { listerEtudiants } from '../api/promotions'
import { deposerExercice } from '../api/exercices'
import { ErreurApi } from '../api/client'
import type { Etudiant, ExerciceCree, StatutExercice } from '../types'

/**
 * Ecran Etudiant : deposer le lien de son exercice (EF5).
 *
 * Remarque d'ergonomie : le contrat impose `{ sessionId, etudiantId, lien }`, donc
 * l'etudiant doit connaitre le numero de session. C'est coherent avec Q1 (aucune
 * authentification) : le formateur annonce la session ouverte, et son ecran affiche
 * deja « Session #N ». Aucune regle metier n'est dupliquee ici : le lien est accepte
 * ou refuse par le serveur, qui decide aussi du statut.
 */
export function DeposerExercice() {
  const [promotionId, setPromotionId] = useState(1)
  const [etudiants, setEtudiants] = useState<Etudiant[]>([])
  const [etudiantId, setEtudiantId] = useState('')
  const [sessionId, setSessionId] = useState('')
  const [lien, setLien] = useState('')
  const [exercice, setExercice] = useState<ExerciceCree | null>(null)
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
      setExercice(
        await deposerExercice({
          sessionId: Number(sessionId),
          etudiantId: Number(etudiantId),
          lien,
        }),
      )
    } catch (e) {
      setExercice(null)
      setErreur(messageDe(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="carte">
      <h2>Deposer mon exercice</h2>
      <p className="aide">
        Le depot reste possible jusqu&apos;a la cloture de la session par le formateur,
        meme si le code de presence a expire (Q12). Un seul exercice par session.
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
          Session annoncee par le formateur
          <input
            type="number"
            min={1}
            value={sessionId}
            placeholder="7"
            onChange={(evenement) => setSessionId(evenement.target.value)}
            required
          />
        </label>

        <label className="champ">
          Lien de mon exercice
          <input
            type="url"
            value={lien}
            maxLength={500}
            placeholder="https://…/mon-exercice.pdf"
            onChange={(evenement) => setLien(evenement.target.value)}
            required
          />
        </label>

        <button type="submit" disabled={envoi || etudiantId === '' || sessionId === ''}>
          {envoi ? 'Depot…' : 'Deposer mon exercice'}
        </button>
      </form>

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {exercice !== null && (
        <div className="resultat" aria-live="polite">
          <p className="succes">
            Exercice #{exercice.id} depose. Statut : {libelleStatut(exercice.statut)}.
          </p>
        </div>
      )}
    </section>
  )
}

/** Traduit le statut renvoye par l'API, sans jamais le recalculer. */
function libelleStatut(statut: StatutExercice): string {
  switch (statut) {
    case 'EN_ATTENTE':
      return 'en attente de relecture'
    case 'SANS_RELECTEUR':
      return 'sans relecteur : le formateur doit en designer un'
    case 'RELU':
      return 'relu'
  }
}

/** Traduit une erreur en message affichable, sans jamais inventer de texte. */
function messageDe(e: unknown): string {
  return e instanceof ErreurApi
    ? `[${e.code}] ${e.message}`
    : 'Une erreur inattendue est survenue.'
}
