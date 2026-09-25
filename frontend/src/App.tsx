import { useState } from 'react'
import { OuvrirSession } from './pages/OuvrirSession'
import { ListeEtudiants } from './pages/ListeEtudiants'

type Onglet = 'session' | 'etudiants'

/**
 * Racine de l'application.
 *
 * v0.1 : ouvrir une session (EF1) et consulter la liste des etudiants (EF15).
 * Les autres ecrans arrivent avec les issues suivantes, dans l'ordre du backlog
 * (docs/issues/README.md). La navigation reste locale tant qu'il n'y a que deux
 * ecrans : une bibliotheque de routage serait une dependance sans benefice ici.
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
          className={onglet === 'etudiants' ? 'onglet actif' : 'onglet'}
          onClick={() => setOnglet('etudiants')}
        >
          Etudiants de la promotion
        </button>
      </nav>

      {onglet === 'session' ? <OuvrirSession /> : <ListeEtudiants />}

      <footer className="pied">
        <p>Version 0.1 — socle et ouverture de session.</p>
      </footer>
    </main>
  )
}
