# KFOKAM48

Présence aux sessions de cours, dépôt d'exercices et relecture entre pairs.

- **Backend** : Java 17, Spring Boot, Maven (wrapper `mvnw` commité), Flyway, PostgreSQL.
- **Frontend** : React + TypeScript, construit avec Vite.
- **Contrat** : `api/contrat.yaml` fait foi pour les chemins, verbes et codes HTTP.
- **Décisions** : `docs/CAHIER_DES_CHARGES.md` (exigences, règles de gestion, arbitrages).
- **Journal** : `JOURNAL.md`.

## Prérequis

| Outil | Version | Remarque |
|---|---|---|
| JDK | 17 ou plus récent | le projet compile en `release 17` |
| Node.js | 18 ou plus récent | pour le frontend |
| PostgreSQL | 14 ou plus récent | **uniquement** pour la démonstration ; les tests n'en ont pas besoin |

## Base de données de développement

Créer une fois la base et le rôle local (commandes à exécuter hors du projet, avec les
droits administrateur) :

```bash
sudo -u postgres psql -c "CREATE ROLE kfokam48 LOGIN PASSWORD 'kfokam48';"
sudo -u postgres psql -c "CREATE DATABASE kfokam48 OWNER kfokam48;"
```

Ces identifiants sont des valeurs de **développement local** et sont surchargeables par les
variables d'environnement `KFOKAM48_DB_URL`, `KFOKAM48_DB_USER` et `KFOKAM48_DB_PASSWORD`.
Aucun secret réel n'est commité : `.env` et les fichiers `application-local.*` sont ignorés
par Git.

Le schéma et les données de référence sont créés par Flyway au premier démarrage
(`backend/src/main/resources/db/migration`). Hibernate ne modifie jamais le schéma :
`ddl-auto=validate` en permanence.

## Backend

```bash
cd backend
./mvnw spring-boot:run     # démarre sur http://localhost:8080
```

Vérification complète (compilation, tests unitaires et tests d'intégration) :

```bash
cd backend
./mvnw verify
```

Les tests tournent sur une base **H2 en mémoire** : aucun PostgreSQL n'est nécessaire, et
les migrations Flyway réelles sont jouées à chaque exécution.

## Frontend

```bash
cd frontend
npm install
npm run dev                # démarre sur http://localhost:5173
```

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
JOURNAL.md                           journal de bord : fait / blocage / vérification
frontend/src/api/                    couche d'appel API unique (aucun fetch dispersé)
frontend/src/pages/                  écrans, un fichier par parcours
```

## Conventions

- Une branche par issue (`feature/<nom-court>`), une PR par branche, un commit par idée.
- Le message de commit se termine par `Closes #N`.
- Aucune requête base dans un contrôleur, aucune entité JPA exposée en JSON.
- Toute erreur sort au format `{ "code": "...", "message": "..." }`, sans trace technique.
