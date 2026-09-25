**Priorité** : Must
**Exigences** : ENF1, ENF3, ENF4, ENF5, ENF11, EF15 · **Règles** : RG1, RG19
**Cahier des charges** : §2.1, §7.6 (T6), §8, §10

## Résultat attendu

Le projet se lance sur un poste vierge, la base est créée par migration versionnée, les
promotions et étudiants de référence existent, et **toute** erreur sort au format imposé
`{ "code": "CODE_ERREUR", "message": "..." }` — sans jamais de stack trace.

## Périmètre

- Wrapper Maven `mvnw` / `mvnw.cmd` commité, `./mvnw verify` fonctionnel (ENF1).
- `.gitignore` Java + JavaScript posé **avant** le premier commit de code (ENF11).
- Squelettes de couches : `controller` → `service` → `repository`, plus `dto`, `entity`, `exception`.
- Migrations Flyway créant les 7 tables de `docs/diagrammes/D2.md` (V1) et les données de
  référence promotions/étudiants (V2). `ddl-auto=validate`, jamais `update` (ENF5).
- Un unique `@RestControllerAdvice` qui mappe les exceptions vers le format imposé (ENF4),
  avec l'erreur générique `500 ERREUR_INTERNE` pour les cas non prévus.
- `GET /api/promotions/{id}/etudiants` : la liste de noms de Q1 (EF15).

## Critères d'acceptation

- Quand je lance `./mvnw verify` sur un poste sans Maven installé, alors le build passe.
- Quand l'application démarre sur une base vide, alors Flyway crée les 7 tables et insère
  les données de référence, et `ddl-auto=validate` ne signale aucun écart.
- Quand j'appelle `GET /api/promotions/{id}/etudiants` avec une promotion connue, alors la
  réponse est `200` avec la liste triée par nom (`id`, `nom`, `prenom`) ; avec une promotion
  inconnue, alors `404 { "code": "PROMOTION_INCONNUE" }`.
- Quand une exception inattendue est levée dans un service, alors la réponse est
  `500 { "code": "ERREUR_INTERNE" }` avec un message générique et **aucune** stack trace.

## Definition of Done

Voir §10 du cahier des charges : tests succès **et** échec, aucun accès base dans un
contrôleur, aucune entité JPA sérialisée, schéma par Flyway, `./mvnw verify` vert, 3 lignes
ajoutées à `JOURNAL.md`.
