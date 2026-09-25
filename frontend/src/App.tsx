import { ListeEtudiants } from './pages/ListeEtudiants'

/**
 * Racine de l'application.
 *
 * v0.1 : seul l'ecran du referentiel est present. Les parcours Formateur,
 * Etudiant et Relecteur viennent avec les issues suivantes, dans l'ordre du
 * backlog (docs/issues/README.md).
 */
export function App() {
  return (
    <main className="page">
      <header className="entete">
        <h1>KFOKAM48</h1>
        <p>Presence aux sessions, depot d'exercices et relecture entre pairs.</p>
      </header>
      <ListeEtudiants />
      <footer className="pied">
        <p>Version 0.1 — socle : base versionnee, erreurs normalisees, referentiel.</p>
      </footer>
    </main>
  )
}
