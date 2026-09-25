**Priorité** : Must
**Exigences** : EF14 · **Règles** : RG21 · **Questions client** : Q10, Q11, Q12
**Contrat** : `POST /api/sessions/{id}/cloture` · **Décision** : §7.5

## Résultat attendu

Le formateur ferme la session quand il l'estime terminé : plus aucun dépôt ni aucune
présence, mais les relectures déjà confiées restent rendables.

## Périmètre

- Horodatage de la clôture (`cloture_at`) et passage de la session à l'état fermé.
- Refus des présences et des dépôts/remplacements sur une session clôturée
  (`409 SESSION_CLOTUREE`).
- Maintien du rendu de note pour les relectures déjà assignées (RG21).
- Clôture idempotente : une seconde clôture ne produit pas d'erreur technique.

## Critères d'acceptation

- Quand le formateur clôture une session ouverte, alors la réponse est `200` avec
  `clotureAt` renseigné.
- Quand un étudiant tente de marquer sa présence sur une session clôturée, alors la réponse
  est `409 { "code": "SESSION_CLOTUREE" }`.
- Quand un étudiant tente de déposer ou de remplacer le lien d'un exercice sur une session
  clôturée, alors la réponse est `409 { "code": "SESSION_CLOTUREE" }`.
- Quand un relecteur rend une note sur une relecture assignée avant la clôture, alors la
  réponse est `200` : la clôture ne bloque pas le rendu (RG21).
- Quand le formateur clôture une session déjà clôturée, alors la session reste dans le même
  état et la réponse est cohérente (aucune erreur technique, même date de clôture).
- Quand une session inconnue est visée, alors la réponse est
  `404 { "code": "SESSION_INTROUVABLE" }`.

## Note

Q10 affirmait qu'une note reste modifiable jusqu'à la clôture ; cette réponse a été écartée
au profit de Q15 (§7.1). La clôture ne sert donc plus à figer les notes — elles le sont dès
l'envoi — mais à fermer les dépôts et les présences (RG21), conformément à Q12.
