# KFOKAM48

Présence aux sessions de cours, dépôt d'exercices et relecture entre pairs — plateforme
de la direction de la formation KFOKAM48.

- **Backend** : Java 17, Spring Boot, Maven (wrapper `mvnw` commité), Flyway, base H2
  (mode PostgreSQL) en développement — aucun SGBD à installer.
- **Frontend** : React + TypeScript avec Vite — **pourquoi ce choix** : la saisie d'un
  code à 6 caractères pendant le cours exige un retour immédiat et un état local simple
  (écran → API → message), qu'un SPA léger rend naturel, sans framework lourd à apprendre
  ni build complexe.
- **Contrat** : `api/contrat.yaml` fait foi pour les chemins, verbes et codes HTTP.
- **Décisions** : `docs/CAHIER_DES_CHARGES.md` (exigences, règles de gestion, arbitrages).
- **Journal** : `docs/JOURNAL.md`.

## Démarrage en 3 commandes (clone vierge)

Prérequis : **Java 17** (JDK) et **Node.js 18+** installés. Rien d'autre — aucun SGBD,
aucun `docker` requis.

```bash
cd backend && ./mvnw spring-boot:run    # 1. API sur http://localhost:8080
cd frontend && npm install              # 2. dépendances du frontend (une seule fois)
cd frontend && npm run dev              # 3. interface sur http://localhost:5173
```

**Données de démonstration chargées automatiquement** : au premier démarrage, Flyway
crée le schéma et insère le référentiel (2 promotions, 16 étudiants) — la liste de Q1
n'est jamais vide et le tableau de bord s'affiche dès l'ouverture. Testé depuis un clone
vierge : API prête en quelques secondes, présence marquée avec le code dicté, tableau à
jour.

## Base de données

**H2, en fichier persistant `backend/data/kfokam48`** (mode PostgreSQL) : rien à
installer. Le dossier `data/` est local et ignoré par Git ; supprimez-le pour repartir
d'une base vierge.

Pour brancher un vrai PostgreSQL, surchargez `KFOKAM48_DB_URL`, `KFOKAM48_DB_USER` et
`KFOKAM48_DB_PASSWORD` — aucune autre modification n'est nécessaire, et aucun secret
réel n'est commité (`.env` et `application-local.*` sont ignorés).

Hibernate ne modifie jamais le schéma : `ddl-auto=validate` en permanence, le schéma
appartient aux migrations Flyway (`backend/src/main/resources/db/migration` : V1 schéma,
V2 données de référence, V3 audit des assignations manuelles), les mêmes qu'exécutent
les tests sur H2 en mémoire.

## Vérification complète

```bash
cd backend && ./mvnw verify    # 149 tests : 43 unitaires + 106 d'intégration
cd frontend && npm run build   # tsc strict + build Vite
```

Les tests tournent sur une base **H2 en mémoire** : rien à installer, et les migrations
Flyway réelles sont jouées à chaque exécution.

En développement, Vite redirige `/api` vers `http://localhost:8080` (voir
`frontend/vite.config.ts`) : aucune configuration CORS n'est nécessaire.

## Structure

```
api/contrat.yaml                     contrat d'API (fait foi)
backend/                             Spring Boot : controller / service / repository / dto
  src/main/resources/db/migration/   migrations Flyway (V1 schéma, V2 référentiel, V3 audit)
  src/test/                          tests unitaires (*Test) et d'intégration (*IT)
docs/                                cahier des charges, diagrammes, backlog, journal, changelog
CLIENT.md                            besoin brut du client (16 Q/R), conservé verbatim
frontend/src/api/                    couche d'appel API unique (aucun fetch dispersé)
frontend/src/pages/                  écrans, un fichier par parcours
CHANGELOG.md                         historique livré, aligné sur les commits et jalons réels
SOUMISSION.md                        notice de soumission (identité, démarrage, décisions)
```

## Conventions

- Une branche par issue (`feature/<nom-court>`), une PR par branche, un commit par idée.
- Le message de commit se termine par `Closes #N`.
- Aucune requête base dans un contrôleur, aucune entité JPA exposée en JSON.
- Toute erreur sort au format `{ "code": "...", "message": "..." }`, sans trace technique.
