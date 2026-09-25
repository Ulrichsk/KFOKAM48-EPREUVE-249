**Priorité** : Must
**Exigences** : EF5 · **Règles** : RG15, RG19, RG22 · **Questions client** : Q12
**Contrat** : `POST /api/exercices`

## Résultat attendu

Un étudiant dépose le lien de son exercice pour une session, sans risque de doublon,
y compris le soir même après l'expiration du code de présence.

## Périmètre

- Création d'un exercice (session + étudiant + lien) au statut initial.
- Contrôles : lien valide (URL http/https, 500 caractères max), session non clôturée,
  étudiant de la promotion de la session, pas de second dépôt pour la même session.
- Unicité garantie par `UNIQUE (session_id, etudiant_id)`.

## Critères d'acceptation

- Quand un étudiant dépose un lien valide sur une session non clôturée, alors la réponse
  est `201` avec `id` et `statut`.
- Quand un étudiant dépose un second exercice pour la même session, alors la réponse est
  `409 { "code": "EXERCICE_DEJA_DEPOSE" }` et le premier dépôt reste intact.
- Quand le lien est vide, relatif ou mal formé, alors la réponse est
  `400 { "code": "LIEN_INVALIDE" }`.
- Quand la session est clôturée, alors la réponse est `409 { "code": "SESSION_CLOTUREE" }`.
- Quand le code de présence est expiré mais la session non clôturée, alors le dépôt est
  **accepté** : Q12 autorise explicitement le dépôt jusqu'à la clôture (RG15).

## Hors périmètre de cette issue

Le tirage du relecteur est traité par l'issue « Trouver automatiquement un relecteur » ;
ici, le statut renvoyé peut être provisoire tant que le tirage n'est pas implémenté.
