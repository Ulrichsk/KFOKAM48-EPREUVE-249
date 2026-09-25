**Priorité** : Must
**Exigences** : EF8 · **Règles** : RG6, RG10, RG14 · **Questions client** : Q5, Q11
**Contrat** : `POST /api/exercices/{id}/relecteur` · **Décision** : §7.4

## Résultat attendu

Aucun exercice ne peut rester invisible : si le tirage n'a trouvé personne, le formateur
le voit et peut débloquer la situation lui-même.

## Périmètre

- Statut `SANS_RELECTEUR` lorsque aucun candidat éligible n'existe (personne n'est présent,
  ou le seul présent est l'auteur, exclu par Q5).
- Visibilité de ces exercices dans le tableau du formateur, parmi les relectures en attente.
- Assignation manuelle : le formateur désigne un relecteur, ce qui crée la relecture avec
  `assigne_par = "FORMATEUR"` et fait passer l'exercice au statut `EN_ATTENTE`.

## Critères d'acceptation

- Quand un exercice est déposé sans aucun candidat éligible, alors la réponse de dépôt
  indique `SANS_RELECTEUR` et l'exercice apparaît comme relecture en attente au tableau.
- Quand le formateur assigne un relecteur valide, alors la relecture est créée, le statut
  devient `EN_ATTENTE` et `assigne_par` vaut `FORMATEUR`.
- Quand le formateur tente d'assigner l'auteur de l'exercice, alors la réponse est
  `403 { "code": "AUTO_EVALUATION_INTERDITE" }` (Q5).
- Quand le formateur tente d'assigner un étudiant qui n'est ni présent ni de la promotion,
  ou un exercice déjà relu, alors la demande est refusée avec une erreur normalisée.
- Quand un exercice `SANS_RELECTEUR` n'est jamais assigné, alors il reste visible au
  tableau et ne disparaît jamais silencieusement (Q11).

## Choix documenté

Le formateur **ne peut pas** relire lui-même un exercice : seule une assignation à un pair
est possible (§7.4). Cela préserve la relecture entre pairs (Q7) et l'anonymat perçu (Q8).
