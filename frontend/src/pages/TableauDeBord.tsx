import { useCallback, useEffect, useState } from 'react'
import { obtenirTableau, designerRelecteur } from '../api/tableau'
import { listerEtudiants } from '../api/promotions'
import { ErreurApi } from '../api/client'
import type { Etudiant, LigneTableau } from '../types'

/**
 * Ecran Formateur : le tableau de bord d'une promotion (EF12, Q16).
 *
 * Une ligne par etudiant, meme sans aucune activite. La moyenne vient de l'API
 * (RG18) : cet ecran ne la recalcule jamais. Les presences ajoutees a la main
 * restent distinguees (Q14), et les relectures en attente restent visibles (Q11).
 *
 * Changement de besoin (enveloppe etape 3, issue 16) : la ligne d'un etudiant
 * dont les exercices sont restes SANS_RELECTEUR porte la liste de ces exercices
 * et permet de designer un relecteur parmi les etudiants de la promotion, autre
 * que l'auteur. L'ecran ne duplique aucune regle : l'API refuse elle-meme
 * l'auteur (403 AUTO_EVALUATION_INTERDITE), un etudiant hors promotion (403
 * ACCES_REFUSE) ou un exercice deja assigne (409), et ses messages sont affiches
 * tels quels. Apres un succes, le tableau est recharge : l'exercice a quitte la
 * liste des sans-relecteur.
 */
export function TableauDeBord() {
  const [promotionId, setPromotionId] = useState(1)
  const [lignes, setLignes] = useState<LigneTableau[]>([])
  const [etudiants, setEtudiants] = useState<Etudiant[]>([])
  const [chargement, setChargement] = useState(false)
  const [exerciceVise, setExerciceVise] = useState<number | null>(null)
  const [relecteurChoisi, setRelecteurChoisi] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [succes, setSucces] = useState<string | null>(null)
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

  const chargerEtudiants = useCallback(async (idPromotion: number) => {
    try {
      setEtudiants(await listerEtudiants(idPromotion))
    } catch {
      // La liste sert seulement a la designation manuelle : en cas d'echec, les
      // lignes du tableau restent affichees et le message de l'API apparaitra au
      // moment de la designation, s'il y en a une.
      setEtudiants([])
    }
  }, [])

  useEffect(() => {
    void charger(promotionId)
    void chargerEtudiants(promotionId)
    setExerciceVise(null)
    setRelecteurChoisi('')
  }, [charger, chargerEtudiants, promotionId])

  async function designer(exerciceId: number) {
    if (relecteurChoisi === '') {
      return
    }
    setEnvoi(true)
    setErreur(null)
    setSucces(null)
    try {
      const assignee = await designerRelecteur(exerciceId, {
        relecteurId: Number(relecteurChoisi),
      })
      setSucces(
        `Relecteur designe pour l'exercice #${assignee.exerciceId} : il est desormais en attente de sa note.`,
      )
      setExerciceVise(null)
      setRelecteurChoisi('')
      await charger(promotionId)
    } catch (e) {
      setErreur(messageDe(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="carte">
      <h2>Tableau de bord de la promotion</h2>
      <p className="aide">
        Une ligne par etudiant, meme sans aucune activite (Q16). La moyenne des notes
        recues est calculee par l&apos;API, jamais ici. Les presences ajoutees a la main
        restent distinguees (Q14). Sur une ligne sans relecteur, designez directement un
        relecteur parmi la promotion.
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

      {succes !== null && (
        <p className="succes" role="status">
          {succes}
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
              <th>Exercices sans relecteur</th>
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
                <td>
                  {ligne.exercicesSansRelecteur.length === 0 ? (
                    <span className="identifiant">—</span>
                  ) : (
                    ligne.exercicesSansRelecteur.map((exerciceId) => (
                      <div key={exerciceId} className="action-ligne">
                        <button
                          type="button"
                          className="onglet"
                          onClick={() => {
                            setSucces(null)
                            setErreur(null)
                            setExerciceVise(exerciceVise === exerciceId ? null : exerciceId)
                          }}
                        >
                          Exercice #{exerciceId} : designer un relecteur
                        </button>
                        {exerciceVise === exerciceId && (
                          <form
                            className="formulaire"
                            onSubmit={(evenement) => {
                              evenement.preventDefault()
                              void designer(exerciceId)
                            }}
                          >
                            <label className="champ">
                              Relecteur (autre que l&apos;auteur)
                              <select
                                value={relecteurChoisi}
                                onChange={(evenement) => setRelecteurChoisi(evenement.target.value)}
                                disabled={envoi}
                                required
                              >
                                <option value="">Choisir un etudiant…</option>
                                {etudiants
                                  .filter((candidat) => candidat.id !== ligne.etudiantId)
                                  .map((candidat) => (
                                    <option key={candidat.id} value={candidat.id}>
                                      {candidat.nom} {candidat.prenom}
                                    </option>
                                  ))}
                              </select>
                            </label>
                            <button type="submit" disabled={envoi || relecteurChoisi === ''}>
                              {envoi ? 'Designation…' : 'Confirmer la designation'}
                            </button>
                          </form>
                        )}
                      </div>
                    ))
                  )}
                </td>
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
