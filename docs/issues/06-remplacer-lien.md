**Priorité** : Should
**Exigences** : EF6 · **Règles** : RG16, RG20, RG21 · **Questions client** : Q13
**Contrat** : `PUT /api/exercices/{id}`

## Résultat attendu

Un étudiant qui s'est trompé de lien peut le corriger — mais seulement tant que personne
n'a commencé à relire son travail.

## Périmètre

- Remplacement du lien par l'auteur uniquement.
- Verrouillage définitif dès que le relecteur a consulté le lien (`lien_consulte_at` non nul).
- Refus après clôture de la session.

## Critères d'acceptation

- Quand l'auteur remplace son lien et qu'aucun relecteur n'a encore ouvert le lien, alors
  la réponse est `200`, le nouveau lien est enregistré et l'ancien n'est plus consultable.
- Quand le relecteur a déjà ouvert le lien, alors la réponse est
  `409 { "code": "RELECTURE_COMMENCEE" }` et l'ancien lien reste en place.
- Quand un autre étudiant que l'auteur tente de remplacer le lien, alors la réponse est
  `403 { "code": "ACCES_REFUSE" }`.
- Quand la session est clôturée, alors la réponse est `409 { "code": "SESSION_CLOTUREE" }`.
- Quand le nouveau lien est invalide, alors la réponse est `400 { "code": "LIEN_INVALIDE" }`
  et l'ancien lien reste en place.

## Dépendance

« Personne n'a commencé à relire » n'a de sens que si la consultation du lien est tracée :
cette issue dépend de « Consulter le lien de l'exercice à relire » (renseignement de
`lien_consulte_at`) et du tirage automatique du relecteur.
