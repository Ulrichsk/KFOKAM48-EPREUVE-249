**Priorité** : Must
**Exigences** : EF1 · **Règles** : RG1, RG2 · **Questions client** : Q2
**Contrat** : `POST /api/sessions`

## Résultat attendu

Le formateur ouvre une session de cours et repart avec un code à dicter à voix haute,
utilisable pendant un quart d'heure.

## Périmètre

- Création d'une session rattachée à une promotion (titre + promotionId).
- Génération d'un code aléatoire de 6 caractères (jeu restreint, RG1) unique parmi les
  sessions non clôturées.
- `expirationAt = ouvertureAt + 15 minutes` (Q2, RG2).

## Critères d'acceptation

- Quand le formateur envoie `titre` et `promotionId` valides, alors la réponse est `201`
  avec `id`, `code`, `ouvertureAt`, `expirationAt`, et `expirationAt - ouvertureAt` vaut
  exactement 15 minutes.
- Quand `titre` est absent, vide ou blanc, alors la réponse est
  `400 { "code": "CHAMP_MANQUANT" }`.
- Quand `promotionId` ne correspond à aucune promotion, alors la réponse est
  `404 { "code": "PROMOTION_INCONNUE" }`.
- Quand deux sessions sont ouvertes à la suite, alors elles ont des codes différents.

## Refusé volontairement

Aucune notion de fin de session à ce stade : la clôture est l'issue suivante, et
l'expiration du code ne ferme pas les dépôts (décision §7.5 du cahier des charges).
