**Priorité** : Must
**Exigences** : EF7 · **Règles** : RG6, RG7, RG8, RG9 · **Questions client** : Q5, Q6, Q7
**Contrat** : inclus dans `POST /api/exercices` · **Décisions** : §7.2, §7.3

## Résultat attendu

Chaque exercice déposé est confié à un pair, désigné au hasard, jamais à son auteur.

## Périmètre

- Tirage effectué **immédiatement au dépôt**, dans la même transaction (décision §7.3).
- Candidats éligibles : étudiants ayant une présence à la session de l'exercice, **quelle
  que soit la source** de cette présence (décision §7.2), à l'exclusion de l'auteur (Q5).
- Un seul relecteur par exercice, garanti par `UNIQUE (exercice_id)` sur la relecture (Q6).
- Relecture créée au statut `EN_ATTENTE` avec `assigne_par = "SYSTEME"`.

## Critères d'acceptation

- Quand un exercice est déposé et qu'au moins un étudiant présent autre que l'auteur
  existe, alors une relecture est créée pour exactement un relecteur, différent de l'auteur.
- Quand le tirage a lieu, alors l'exercice prend le statut `EN_ATTENTE` et la réponse de
  dépôt le renvoie.
- Quand un étudiant a été ajouté manuellement par le formateur (source `FORMATEUR`), alors
  il peut être tiré au sort : l'origine de la présence ne filtre pas l'éligibilité.
- Quand aucun étudiant éligible n'existe, alors **aucune** relecture n'est créée et
  l'exercice prend le statut `SANS_RELECTEUR` (traité par l'issue « Voir et débloquer les
  exercices sans relecteur »).
- Quand plusieurs exercices sont déposés, alors chaque tirage est indépendant et deux
  exercices différents peuvent avoir des relecteurs différents.

## Vérification attendue

Un test à candidats connus vérifie que le tirage est **aléatoire** (plusieurs exécutions
produisent des relecteurs variés) et qu'il est **toujours** exclu de désigner l'auteur.
