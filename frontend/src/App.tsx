import { useState } from 'react'
import { OuvrirSession } from './pages/OuvrirSession'
import { MarquerPresence } from './pages/MarquerPresence'
import { ListeEtudiants } from './pages/ListeEtudiants'

type Onglet = 'session' | 'presence' | 'etudiants'

/**
 * Racine de l'application.
 *
 * v0.2 : ouvrir une session (EF1), marquer sa presence (EF2) et consulter la liste
 * des etudiants (EF15). Les ecrans du relecteur et le tableau de bord arrivent avec
 * les issues suivantes, dans l'ordre du backlog (docs/issues/README.md).
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
          className={onglet === 'etudiants' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('etudiants')}
        >
          Etudiants de la promotion
        </button>
      </nav>

      {onglet === 'session' && <OuvrirSession />}
      {onglet === 'presence' && <MarquerPresence />}
      {onglet === 'etudiants' && <ListeEtudiants />}

      <footer className="pied">
        <p>Version 0.2 — socle, ouverture de session et marquage de presence.</p>
      </footer>
    </main>
  )
}
