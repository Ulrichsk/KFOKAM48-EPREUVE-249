import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { listerEtudiants } from '../api/promotions'
import { listerMesRelectures, consulterRelecture, rendreRelecture } from '../api/relectures'
import { ErreurApi } from '../api/client'
import type { Etudiant, RelectureResume, RelectureDetail, RelectureRendue } from '../types'

/**
 * Ecran Relecteur : voir les relectures confiees, ouvrir le lien a relire (EF9) et
 * rendre une note sur 20 avec un commentaire (EF10).
 *
 * Aucune regle metier ici : c'est le serveur qui refuse un intrus (403), une note
 * invalide (400 NOTE_INVALIDE) ou un second envoi (409 RELECTURE_DEJA_RENDUE) ; ses
 * messages sont affiches tels quels. La note envoyee est definitive (Q15) : l'ecran
 * l'annonce, sans proposer aucune modification — il n'existe pas de tel endpoint.
 */
export function RelireExercice() {
  const [promotionId, setPromotionId] = useState(1)
  const [etudiants, setEtudiants] = useState<Etudiant[]>([])
  const [etudiantId, setEtudiantId] = useState('')
  const [relectures, setRelectures] = useState<RelectureResume[]>([])
  const [detail, setDetail] = useState<RelectureDetail | null>(null)
  const [rendue, setRendue] = useState<RelectureRendue | null>(null)
  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [chargementListe, setChargementListe] = useState(false)
  const [chargementRelectures, setChargementRelectures] = useState(false)
  const [ouverture, setOuverture] = useState(false)
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

  const chargerRelectures = useCallback(async (idEtudiant: number) => {
    setChargementRelectures(true)
    setErreur(null)
    setDetail(null)
    setRendue(null)
    try {
      setRelectures(await listerMesRelectures(idEtudiant))
    } catch (e) {
      setRelectures([])
      setErreur(messageDe(e))
    } finally {
      setChargementRelectures(false)
    }
  }, [])

  useEffect(() => {
    void chargerListe(promotionId)
  }, [chargerListe, promotionId])

  useEffect(() => {
    if (etudiantId !== '') {
      void chargerRelectures(Number(etudiantId))
    }
  }, [chargerRelectures, etudiantId])

  const enAttente = relectures.filter((relecture) => relecture.statut === 'EN_ATTENTE')
  const dejaRendues = relectures.filter((relecture) => relecture.statut === 'RENDUE')

  async function ouvrirLeLien(relectureId: number) {
    setOuverture(true)
    setErreur(null)
    setRendue(null)
    try {
      // La premiere consultation horodate le lien cote serveur (Q13) : l'auteur
      // ne pourra plus le remplacer. L'ecran ne fait qu'afficher ce que renvoie l'API.
      setDetail(await consulterRelecture(relectureId, Number(etudiantId)))
      setNote('')
      setCommentaire('')
    } catch (e) {
      setDetail(null)
      setErreur(messageDe(e))
    } finally {
      setOuverture(false)
    }
  }

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault()
    if (detail === null) {
      return
    }
    setEnvoi(true)
    setErreur(null)
    try {
      const resultat = await rendreRelecture(detail.id, Number(etudiantId), {
        note: Number(note),
        commentaire: commentaire.trim() === '' ? null : commentaire.trim(),
      })
      setRendue(resultat)
      setDetail(null)
      await chargerRelectures(Number(etudiantId))
    } catch (e) {
      setErreur(messageDe(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="carte">
      <h2>Faire une relecture</h2>
      <p className="aide">
        Vous ne verrez jamais qui a ecrit l&apos;exercice, et l&apos;auteur ne saura pas qui
        l&apos;a note (Q8). La note, entiere de 0 a 20, est definitive des son envoi (Q15).
      </p>

      <form className="formulaire">
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
      </form>

      {chargementRelectures && <p className="etat">Chargement de vos relectures…</p>}

      {!chargementRelectures && relectures.length === 0 && etudiantId !== '' && (
        <p className="etat">Aucune relecture ne vous est confiée pour le moment.</p>
      )}

      {enAttente.length > 0 && (
        <>
          <h3>À relire</h3>
          <ul className="liste">
            {enAttente.map((relecture) => (
              <li key={relecture.id}>
                <span>
                  Exercice #{relecture.exerciceId}{' '}
                  <span className="identifiant">(relecture #{relecture.id})</span>
                </span>
                <button
                  type="button"
                  className="onglet"
                  onClick={() => void ouvrirLeLien(relecture.id)}
                  disabled={ouverture}
                >
                  {ouverture ? 'Ouverture…' : 'Ouvrir le lien à relire'}
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {dejaRendues.length > 0 && (
        <>
          <h3>Déjà rendues</h3>
          <ul className="liste">
            {dejaRendues.map((relecture) => (
              <li key={relecture.id}>
                <span>
                  Exercice #{relecture.exerciceId} <span className="identifiant">(relecture #{relecture.id})</span>
                </span>
                <span className="identifiant">note envoyée</span>
              </li>
            ))}
          </ul>
        </>
      )}

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {detail !== null && (
        <div className="resultat">
          <h3>Exercice #{detail.exerciceId}</h3>
          <dl className="details">
            <dt>Session</dt>
            <dd>{detail.titreSession}</dd>
            <dt>Statut</dt>
            <dd>en attente de votre note</dd>
            {detail.lienConsulteAt !== null && (
              <>
                <dt>Lien consulté le</dt>
                <dd>{new Date(detail.lienConsulteAt).toLocaleString('fr-FR')}</dd>
              </>
            )}
          </dl>
          <p>
            <a href={detail.lien} target="_blank" rel="noreferrer">
              Ouvrir le lien de l&apos;exercice
            </a>
          </p>

          <form onSubmit={soumettre} className="formulaire">
            <label className="champ">
              Note sur 20 (nombre entier)
              <input
                type="number"
                min={0}
                max={20}
                step={1}
                value={note}
                onChange={(evenement) => setNote(evenement.target.value)}
                required
              />
            </label>
            <label className="champ">
              Commentaire (facultatif)
              <input
                type="text"
                maxLength={2000}
                value={commentaire}
                placeholder="Un mot sur le travail…"
                onChange={(evenement) => setCommentaire(evenement.target.value)}
              />
            </label>
            <button type="submit" disabled={envoi || note === ''}>
              {envoi ? 'Envoi…' : 'Envoyer la note (définitive)'}
            </button>
          </form>
        </div>
      )}

      {rendue !== null && (
        <div className="resultat" aria-live="polite">
          <p className="succes">
            Note de {rendue.note}/20 enregistrée pour l&apos;exercice #{rendue.exerciceId}. Elle
            est définitive (Q15) et l&apos;auteur peut désormais la consulter.
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
