**Priorité** : Must
**Exigences** : EF2, EF3 · **Règles** : RG2, RG3, RG4, RG19 · **Questions client** : Q2, Q3
**Contrat** : `POST /api/presences` · **Diagramme** : `docs/diagrammes/D3.md`

## Résultat attendu

Un étudiant saisit le code dicté et sa présence est enregistrée une seule fois, dans le
temps imparti.

## Périmètre

- Résolution du code vers la session, puis contrôles : session non clôturée, code non
  expiré, étudiant appartenant à la promotion de la session.
- Création de la présence avec `source = "ETUDIANT"`.
- Unicité garantie par la contrainte `UNIQUE (session_id, etudiant_id)`, pas seulement par
  un test applicatif (RG4).

## Critères d'acceptation

- Quand un étudiant saisit un code valide d'une session ouverte non expirée, alors la
  réponse est `201` avec `id`, `sessionId`, `etudiantId`, `source = "ETUDIANT"`.
- Quand le même étudiant resaisit le même code, alors la réponse est
  `409 { "code": "DEJA_PRESENT" }` et **aucune** seconde ligne n'est créée.
- Quand le code ne correspond à aucune session, alors la réponse est
  `400 { "code": "CODE_INCONNU" }`.
- Quand le code appartient à une session dont l'expiration est dépassée, alors la réponse
  est `410 { "code": "CODE_EXPIRE" }` — et jamais `409`, même si l'étudiant était déjà présent.
- Quand la session a été clôturée, alors la réponse est
  `409 { "code": "SESSION_CLOTUREE" }`.
- Quand l'étudiant appartient à une autre promotion que celle de la session, alors la
  présence est refusée (RG19).

## Definition of Done

Les trois scénarios de `docs/diagrammes/D3.md` sont couverts par des tests d'intégration
HTTP vérifiant le code de statut **et** le corps d'erreur exact.
