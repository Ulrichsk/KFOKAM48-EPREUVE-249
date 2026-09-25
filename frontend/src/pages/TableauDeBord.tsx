import { useCallback, useEffect, useState } from 'react'
import { obtenirTableau } from '../api/tableau'
import { ErreurApi } from '../api/client'
import type { LigneTableau } from '../types'

/**
 * Ecran Formateur : le tableau de bord d'une promotion (EF12, Q16).
 *
 * Une ligne par etudiant, meme sans aucune activite. La moyenne vient de l'API
 * (RG18) : cet ecran ne la recalcule jamais. Les presences ajoutees a la main
 * restent distinguees (Q14), et les relectures en attente restent visibles (Q11).
 */
export function TableauDeBord() {
  const [promotionId, setPromotionId] = useState(1)
  const [lignes, setLignes] = useState<LigneTableau[]>([])
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const charger = useCallback(async (idPromotion: number) => {
    setChargement(true)
    setErreur(null)
    try {
      setLignes(await obtenirTableau(idPromotion))
    } catch (e) {
      setLignes([])
      setErreur(messageDe(e))
    } finally {
      setChargement(false)
    }
  }, [])

  useEffect(() => {
    void charger(promotionId)
  }, [charger, promotionId])

  return (
    <section className="carte">
      <h2>Tableau de bord de la promotion</h2>
      <p className="aide">
        Une ligne par etudiant, meme sans aucune activite (Q16). La moyenne des notes
        recues est calculee par l&apos;API, jamais ici. Les presences ajoutees a la main
        restent distinguees (Q14).
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

      {chargement && <p className="etat">Chargement du tableau…</p>}

      {erreur !== null && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {!chargement && erreur === null && lignes.length > 0 && (
        <table className="tableau">
          <thead>
            <tr>
              <th>Etudiant</th>
              <th>Presences</th>
              <th>dont formateur</th>
              <th>Exercices</th>
              <th>Moyenne /20</th>
              <th>Relectures a rendre</th>
            </tr>
          </thead>
          <tbody>
            {lignes.map((ligne) => (
              <tr key={ligne.etudiantId}>
                <td>
                  {ligne.nom} {ligne.prenom}
                </td>
                <td>{ligne.presences}</td>
                <td>{ligne.presencesFormateur}</td>
                <td>{ligne.exercicesDeposes}</td>
                <td>{ligne.moyenne}</td>
                <td>{ligne.relecturesEnAttente}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!chargement && erreur === null && lignes.length === 0 && (
        <p className="etat">Aucun etudiant pour cette promotion.</p>
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
