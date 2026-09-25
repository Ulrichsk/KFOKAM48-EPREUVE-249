# KFOKAM48

Présence aux sessions de cours, dépôt d'exercices et relecture entre pairs.

- **Backend** : Java 17, Spring Boot, Maven (wrapper `mvnw` commité), Flyway, base H2 (mode PostgreSQL) en développement — aucun SGBD à installer.
- **Frontend** : React + TypeScript, construit avec Vite.
- **Contrat** : `api/contrat.yaml` fait foi pour les chemins, verbes et codes HTTP.
- **Décisions** : `docs/CAHIER_DES_CHARGES.md` (exigences, règles de gestion, arbitrages).
- **Journal** : `docs/JOURNAL.md`.
- **Frontend** : React + TypeScript, construit avec Vite.
- **Contrat** : `api/contrat.yaml` fait foi pour les chemins, verbes et codes HTTP.
- **Décisions** : `docs/CAHIER_DES_CHARGES.md` (exigences, règles de gestion, arbitrages).
- **Journal** : `JOURNAL.md`.

## Démarrage en 3 commandes (poste vierge)

```bash
cd backend && ./mvnw spring-boot:run    # 1. API sur http://localhost:8080
cd frontend && npm install              # 2. dépendances du frontend (une seule fois)
cd frontend && npm run dev              # 3. interface sur http://localhost:5173
```

## Base de données

**H2, en fichier persistant `backend/data/kfokam48`** (mode PostgreSQL) : rien à installer,
le schéma et les données de démonstration sont créés par Flyway au premier démarrage
(2 promotions, 16 étudiants — l'application n'est jamais vide, Q1/Q16). Le dossier `data/`
est local et ignoré par Git ; supprimez-le pour repartir d'une base vierge.

Pour brancher un vrai PostgreSQL, surchargez `KFOKAM48_DB_URL`, `KFOKAM48_DB_USER` et
`KFOKAM48_DB_PASSWORD` — aucune autre modification n'est nécessaire, et aucun secret réel
n'est commité (`.env` et `application-local.*` sont ignorés).

Hibernate ne modifie jamais le schéma : `ddl-auto=validate` en permanence, le schéma
appartient aux migrations Flyway (`backend/src/main/resources/db/migration`), les mêmes
qu'exécutent les tests sur H2 en mémoire.

## Vérification complète

```bash
cd backend && ./mvnw verify   # compilation + tests unitaires + tests d'intégration
```

Les tests tournent sur une base **H2 en mémoire** : rien à installer, et les migrations
Flyway réelles sont jouées à chaque exécution.

En développement, Vite redirige `/api` vers `http://localhost:8080` (voir
`frontend/vite.config.ts`) : aucune configuration CORS n'est nécessaire.

## Structure

```
api/contrat.yaml                     contrat d'API (fait foi)
backend/                             Spring Boot : controller / service / repository / dto
  src/main/resources/db/migration/   migrations Flyway (V1 schéma, V2 données de référence)
  src/test/                          tests unitaires (*Test) et d'intégration (*IT)
docs/                                cahier des charges, diagrammes, backlog d'issues
CLIENT.md                            besoin brut du client (16 Q/R), conservé verbatim
docs/JOURNAL.md                      journal de bord : fait / blocage / vérification
frontend/src/api/                    couche d'appel API unique (aucun fetch dispersé)
frontend/src/pages/                  écrans, un fichier par parcours
```

## Conventions

- Une branche par issue (`feature/<nom-court>`), une PR par branche, un commit par idée.
- Le message de commit se termine par `Closes #N`.
- Aucune requête base dans un contrôleur, aucune entité JPA exposée en JSON.
- Toute erreur sort au format `{ "code": "...", "message": "..." }`, sans trace technique.
