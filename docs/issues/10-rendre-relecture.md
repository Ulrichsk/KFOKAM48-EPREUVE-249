**Priorité** : Must
**Exigences** : EF10 · **Règles** : RG11, RG12, RG13, RG21 · **Questions client** : Q9, Q15
**Contrat** : `POST /api/relectures/{id}` · **Décision** : §7.1

## Résultat attendu

Le relecteur envoie une note sur 20 et un commentaire, une seule fois : cette note est
définitive.

## Périmètre

- Saisie d'une note entière entre 0 et 20 et d'un commentaire facultatif (2 000 caractères
  au plus).
- Passage de la relecture au statut `RENDUE`, horodatage de `rendu_at`, mise à jour du
  statut de l'exercice à `RELU`.
- Verrouillage permanent : aucun second envoi n'est accepté, quelle qu'en soit l'origine.

## Critères d'acceptation

- Quand le relecteur désigné envoie une note entière entre 0 et 20, alors la réponse est
  `200`, la relecture est `RENDUE` et l'exercice passe à `RELU`.
- Quand la note est décimale, négative ou supérieure à 20, alors la réponse est
  `400 { "code": "NOTE_INVALIDE" }` et rien n'est enregistré.
- Quand la même relecture est envoyée une seconde fois, alors la réponse est
  `409 { "code": "RELECTURE_DEJA_RENDUE" }` et **la note initialement enregistrée reste
  inchangée** — y compris si le second envoi propose une valeur différente.
- Quand c'est l'auteur de l'exercice qui envoie la note, alors la réponse est
  `403 { "code": "AUTO_EVALUATION_INTERDITE" }`.
- Quand un commentaire est envoyé vide, alors l'envoi est accepté : seul le commentaire est
  facultatif, la note est obligatoire.
- Quand la session a été clôturée, alors l'envoi reste **possible** pour une relecture déjà
  assignée (RG21) ; seule la création de nouveaux dépôts est fermée.

## Choix documenté

Q10 (« le relecteur peut corriger sa note tant que la session n'est pas clôturée ») est
déclarée périmée au profit de Q15 : la note est définitive dès son envoi (§7.1 du cahier
des charges). Le contrat impose d'ailleurs `409 relecture déjà rendue` sur cette opération.
