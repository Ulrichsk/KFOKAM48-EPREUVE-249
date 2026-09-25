import { useState } from 'react'
import { OuvrirSession } from './pages/OuvrirSession'
import { TableauDeBord } from './pages/TableauDeBord'
import { MarquerPresence } from './pages/MarquerPresence'
import { DeposerExercice } from './pages/DeposerExercice'
import { RelireExercice } from './pages/RelireExercice'
import { ListeEtudiants } from './pages/ListeEtudiants'

type Onglet = 'session' | 'presence' | 'depot' | 'relecture' | 'tableau' | 'etudiants'

/**
 * Racine de l'application.
 *
 * Les trois parcours requis sont couverts : formateur (ouvrir une session, tableau
 * de bord), etudiant (marquer sa presence, deposer son exercice), relecteur (faire
 * une relecture) — plus la liste des etudiants de Q1.
 */
export function App() {
  const [onglet, setOnglet] = useState<Onglet>('session')

  return (
    <main className="page">
      <header className="entete">
        <h1>KFOKAM48</h1>
        <p>Presence aux sessions, depot d'exercices et relecture entre pairs.</p>
      </header>

      <nav className="onglets">
        <button
          type="button"
          className={onglet === 'session' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('session')}
        >
          Formateur — ouvrir une session
        </button>
        <button
          type="button"
          className={onglet === 'tableau' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('tableau')}
        >
          Formateur — tableau de bord
        </button>
        <button
          type="button"
          className={onglet === 'presence' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('presence')}
        >
          Etudiant — marquer ma presence
        </button>
        <button
          type="button"
          className={onglet === 'depot' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('depot')}
        >
          Etudiant — deposer mon exercice
        </button>
        <button
          type="button"
          className={onglet === 'relecture' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('relecture')}
        >
          Relecteur — faire une relecture
        </button>
        <button
          type="button"
          className={onglet === 'etudiants' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('etudiants')}
        >
          Etudiants de la promotion
        </button>
      </nav>

      {onglet === 'session' && <OuvrirSession />}
      {onglet === 'tableau' && <TableauDeBord />}
      {onglet === 'presence' && <MarquerPresence />}
      {onglet === 'depot' && <DeposerExercice />}
      {onglet === 'relecture' && <RelireExercice />}
      {onglet === 'etudiants' && <ListeEtudiants />}

      <footer className="pied">
        <p>Version 0.4 — sessions, presence, depot, relecture et tableau de bord.</p>
      </footer>
    </main>
  )
}
