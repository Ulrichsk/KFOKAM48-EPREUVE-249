**Priorité** : Must
**Exigences** : EF12 · **Règles** : RG17, RG18 · **Questions client** : Q11, Q14, Q16
**Contrat** : `GET /api/tableau?promotionId=`

## Résultat attendu

Le formateur ouvre une seule page et voit, pour chaque étudiant de la promotion, ses
présences, ses exercices déposés, sa moyenne et ce qui attend encore d'être relu.

## Périmètre

- Une ligne par étudiant de la promotion, jamais une ligne manquante.
- `presences` : nombre de présences, avec distinction visible des présences ajoutées à la
  main (Q14, RG17).
- `exercicesDeposes` : nombre d'exercices déposés.
- `moyenne` : moyenne des notes reçues, arrondie à une décimale, `0` (ou nul) si aucune
  note (RG18).
- `relecturesEnAttente` : relectures que l'étudiant doit encore rendre, y compris les
  exercices restés `SANS_RELECTEUR` pour lesquels il a été assigné après coup.
- Performance : requêtes agrégées, sans boucle par étudiant (ENF8).

## Critères d'acceptation

- Quand le formateur appelle le tableau avec une promotion connue, alors la réponse est
  `200` avec une entrée par étudiant comprenant `etudiantId`, `nom`, `presences`,
  `exercicesDeposes`, `moyenne`, `relecturesEnAttente`.
- Quand un étudiant n'a ni présence, ni exercice, ni note, alors sa ligne est présente avec
  des valeurs nulles ou à zéro, et non absente.
- Quand un étudiant a reçu deux notes, par exemple 12 et 15, alors la moyenne affichée est
  `13.5` ; quand il n'a reçu aucune note, alors la moyenne reflète l'absence de note.
- Quand un exercice reste sans relecture rendue, alors le formateur le distingue clairement
  d'un exercice relu (Q11) — la ligne de l'auteur et celle du relecteur défaillant sont
  toutes deux informatives.
- Quand une présence a été ajoutée par le formateur, alors le nombre de présences
  `FORMATEUR` est exposé séparément ou signalé, conformément à Q14.
- Quand la promotion est inconnue, alors la réponse est `404 { "code": "PROMOTION_INCONNUE" }`.

## Definition of Done

Un test vérifie l'absence de requête par étudiant (comptage des requêtes SQL), l'exigence
ENF8 étant vérifiable objectivement.
