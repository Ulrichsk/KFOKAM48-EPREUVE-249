import { useCallback, useEffect, useState } from 'react'
import { listerEtudiants } from '../api/promotions'
import { ErreurApi } from '../api/client'
import type { Etudiant } from '../types'

/**
 * Ecran du socle : affiche la liste des etudiants d'une promotion.
 *
 * Il materialise EF15 (« choisir son nom dans la liste », Q1) et sert de patron
 * pour les ecrans suivants : etat de chargement, etat d'erreur utilisant le
 * message renvoye par l'API, etat nominal. Aucune donnee n'est recalculee cote
 * front : tout vient de l'API.
 */
export function ListeEtudiants() {
  const [promotionId, setPromotionId] = useState(1)
  const [etudiants, setEtudiants] = useState<Etudiant[]>([])
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const charger = useCallback(async (idPromotion: number) => {
    setChargement(true)
    setErreur(null)
    try {
      setEtudiants(await listerEtudiants(idPromotion))
    } catch (e) {
      setEtudiants([])
      setErreur(
        e instanceof ErreurApi
          ? `[${e.code}] ${e.message}`
          : 'Une erreur inattendue est survenue.',
      )
    } finally {
      setChargement(false)
    }
  }, [])

  useEffect(() => {
    void charger(promotionId)
  }, [charger, promotionId])

  return (
    <section className="carte">
      <h2>Etudiants de la promotion</h2>
      <p className="aide">
        Q1 : l'etudiant choisit son nom dans cette liste, sans mot de passe. Les donnees
        viennent de <code>GET /api/promotions/&#123;id&#125;/etudiants</code>.
      </p>

      <label className="champ">
        Promotion
        <input
          type="number"
          min={1}
          value={promotionId}
          onChange={(evenement) => setPromotionId(Number(evenement.target.value))}
        />
      </label>

      {chargement && <p className="etat">Chargement…</p>}

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {!chargement && erreur === null && (
        <ul className="liste">
          {etudiants.map((etudiant) => (
            <li key={etudiant.id}>
              <span className="nom">
                {etudiant.nom} {etudiant.prenom}
              </span>
              <span className="identifiant">#{etudiant.id}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
