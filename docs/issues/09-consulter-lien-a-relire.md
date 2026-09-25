**Priorité** : Must
**Exigences** : EF9 · **Règles** : RG16, RG20 · **Questions client** : Q8, Q13
**Contrat** : `GET /api/relectures`, `GET /api/relectures/{id}` · **Trou** : T1, T3 (§7.6)

## Résultat attendu

Le relecteur sait quels exercices lui sont confiés et peut ouvrir le lien à relire, sans
jamais apprendre qui a écrit l'exercice.

## Périmètre

- Liste des relectures d'un étudiant, séparant celles en attente de celles déjà rendues.
- Consultation d'une relecture : le lien de l'exercice, la session et le statut.
- Horodatage de la première consultation (`lien_consulte_at`), qui fige le remplacement du
  lien par l'auteur (Q13, RG16).
- Anonymat : ni le nom de l'auteur, ni le nom du relecteur n'apparaissent dans les réponses
  destinées à un étudiant (Q8, RG20).

## Critères d'acceptation

- Quand le relecteur désigné appelle `GET /api/relectures/{id}`, alors la réponse est `200`
  avec le lien, la session et le statut, **sans** l'identité de l'auteur.
- Quand un autre étudiant que le relecteur désigné appelle la même ressource, alors la
  réponse est `403 { "code": "ACCES_REFUSE" }`.
- Quand la consultation a lieu, alors `lien_consulte_at` est renseigné au premier accès et
  n'est plus modifié ensuite ; avant cette consultation, il reste nul (Q13).
- Quand une relecture est déjà rendue, alors la consultation reste possible mais
  `lien_consulte_at` n'est pas écrasé.
- Quand la relecture demandée n'existe pas, alors la réponse est
  `404 { "code": "RELECTURE_INTROUVABLE" }`.

## Definition of Done

Un test vérifie explicitement qu'aucune réponse ne contient le nom du relecteur ni celui de
l'auteur, l'anonymat de Q8 étant une exigence vérifiable, pas une intention.
