import { useState } from 'react'
import { OuvrirSession } from './pages/OuvrirSession'
import { MarquerPresence } from './pages/MarquerPresence'
import { DeposerExercice } from './pages/DeposerExercice'
import { RelireExercice } from './pages/RelireExercice'
import { ListeEtudiants } from './pages/ListeEtudiants'

type Onglet = 'session' | 'presence' | 'depot' | 'relecture' | 'etudiants'

/**
 * Racine de l'application.
 *
 * v0.3 : ouvrir une session (EF1), marquer sa presence (EF2), deposer le lien de son
 * exercice (EF5) et consulter la liste des etudiants (EF15). Les ecrans du relecteur,
 * le tirage au sort et le tableau de bord arrivent avec les issues suivantes, dans
 * l'ordre du backlog (docs/issues/README.md).
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
      {onglet === 'presence' && <MarquerPresence />}
      {onglet === 'depot' && <DeposerExercice />}
      {onglet === 'relecture' && <RelireExercice />}
      {onglet === 'etudiants' && <ListeEtudiants />}

      <footer className="pied">
        <p>Version 0.3 — socle, sessions, presence et depot d'exercices.</p>
      </footer>
    </main>
  )
}
