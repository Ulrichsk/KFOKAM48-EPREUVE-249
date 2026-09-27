# SOUMISSION — Épreuve finale fullstack KFOKAM48

**Candidat** : Candidat-KF48-YAO-249
**Dépôt** : https://github.com/Ulrichsk/KFOKAM48-EPREUVE-249
**Branche principale** : `main`

---

## 1. Ce que fait l'application

Plateforme de la direction de la formation KFOKAM48 :

1. Le **formateur** ouvre une session de cours et obtient un **code de présence valable 15 minutes** ;
2. L'**étudiant** choisit son **nom dans une liste** (aucun mot de passe, Q1) et marque sa **présence** avec le code ;
3. L'**étudiant** dépose le **lien de son exercice** pour la session (possible même après l'expiration du code, jusqu'à clôture — Q12) ;
4. Un **relecteur** est **tiré au sort parmi les présents** (jamais l'auteur, Q5/Q6/Q7), consulte le **lien à relire** (anonymat de l'auteur, Q8) et rend une **note entière sur 20** avec commentaire — **définitive dès l'envoi** (Q15, arbitrage §7.1) ;
5. Le **formateur** voit le **tableau** de la promotion : présences (dont ajoutées à la main, Q14), exercices déposés, **moyenne des notes reçues**, relectures en attente (Q16) — et peut **clôturer** la session (dépôts et présences fermés, relectures assignées restées rendables, RG21) ; depuis l'enveloppe étape 3, il **désigne un relecteur** aux exercices restés sans relecteur **directement depuis l'écran** du tableau, et chaque désignation manuelle laisse une **trace d'audit** (RG23, migration V3).

## 2. Démarrage en 3 commandes (clone vierge, rien à installer comme SGBD)

```bash
cd backend && ./mvnw spring-boot:run    # 1. API sur http://localhost:8080
cd frontend && npm install              # 2. dépendances du frontend (une seule fois)
cd frontend && npm run dev              # 3. interface sur http://localhost:5173
```

- **Base de données** : H2 en **fichier persistant** `backend/data/kfokam48` (mode PostgreSQL), créée par **Flyway** au premier démarrage avec les **données de démonstration** (2 promotions, 16 étudiants) — l'application n'est jamais vide. Supprimer le dossier `data/` pour repartir de zéro.
- **PostgreSQL optionnel** : surcharger `KFOKAM48_DB_URL`, `KFOKAM48_DB_USER`, `KFOKAM48_DB_PASSWORD` (aucun secret committé).
- **Vérification complète** (tests unitaires + intégration, 149 tests, H2 en mémoire — aucun serveur requis) :

```bash
cd backend && ./mvnw verify
cd frontend && npm run build
```

## 3. Arborescence du dépôt

| Chemin | Contenu |
|---|---|
| `docs/CAHIER_DES_CHARGES.md` | Analyse complète : 10 sections imposées, EF1–EF15, RG1–RG23, arbitrages §7 (dont **Q10/Q15**) |
| `docs/diagrammes/` | D1 cas d'utilisation · D2 modèle de données (normatif Flyway) · D3 séquence « marquer sa présence » · D4 cycle de vie d'un exercice (bonus) |
| `docs/JOURNAL.md` | Journal de bord : fait / blocage / vérification, entrée par issue |
| `docs/issues/` | Backlog détaillé (les **issues GitHub #1–#14** reprennent ce contenu) + repriorisation étape 3 |
| `api/contrat.yaml` | **Contrat d'API fait foi** : 12 chemins, 13 opérations, les 5 opérations imposées incluses, format d'erreur `{code, message}` partout |
| `backend/` | Spring Boot 3.2, Java 17, `mvnw` commité, Flyway (V1 schéma, V2 seed, V3 audit), contrôleur/service/repository stricts, DTO obligatoires, `@RestControllerAdvice` |
| `frontend/` | React + TypeScript (Vite) : 3 écrans requis + tableau de bord avec désignation de relecteur + liste des étudiants ; **couche API unique** (`src/api/`), moyenne **jamais recalculée** côté client |
| `CLIENT.md` | Besoin brut du client (16 Q/R) conservé verbatim |
| `docs/enveloppes/enveloppe-etape3.md` | Enveloppe étape 3 (bug + changement de besoin), **reconstruite localement** avec sa provenance explicitée et ses scénarios de reproduction |
| `docs/REPRIORISATION_BACKLOG.md` | Repriorisation du backlog suite au changement de besoin (point 6 du protocole étape 3) |
| `CHANGELOG.md` | Historique livré, aligné sur les commits et jalons réels |
| `README.md` | Installation testée depuis un clone vierge |
| `SOUMISSION.md` | Ce fichier |

## 4. Tests (le sujet en exige deux ; le projet en compte 149)

- **Unitaires** (43) : règles métier réelles — expiration du code, unicité de présence, tirage au sort (jamais l'auteur), validation de lien, blocage après 5 échecs…
- **Intégration** (106) : un fichier IT par endpoint et un IT par parcours de l'étape 3, sur **H2 en mémoire** avec les vraies migrations Flyway — poste vierge, aucune base locale pré-existante.
- Exemples de vérification de bout en bout : `410 CODE_EXPIRE` prioritaire sur `409 DEJA_PRESENT` (D3), `409 RELECTURE_DEJA_RENDUE` avec note d'origine inchangée (Q15), lien vide sur `PUT /api/exercices/{id}` → `400 LIEN_INVALIDE` (bug #15, d'abord démontré **rouge**), parcours tableau → désignation → tableau avec trace d'audit (#16).

## 5. Organisation Git exigée par le sujet

- **Identité** : tous les commits de code et de documentation signés `Candidat-KF48-YAO-249 <kf48-yao-249@kfokam48.local>` — aucun nom réel nulle part (commits, issues, fichiers). Deux exceptions assumées : les commits de **fusion** de l'étape 3 (3af5f55, 1201d3e), créés via l'API GitHub, portent l'identité du compte de dépôt — leur réécriture exigerait un `push --force` sur `main`, interdit par la convention du projet.
- **Une branche par issue**, un commit par idée, messages explicites se terminant par `Closes #N`, **push après chaque commit**, fusions par PR/merge explicites dans `main`.
- **Jalons vides** : `[JALON] analyse` (premier commit de l'historique), puis `[JALON] v0.1` et `[JALON] v1.0` aux étapes correspondantes.
- `.gitignore` Java+JS posé **avant** tout commit de code ; aucun fichier généré ni secret dans l'historique.

## 6. Décisions d'analyse à connaître avant la correction

| Décision | Justification |
|---|---|
| **Q15 prime sur Q10** : la note est définitive dès l'envoi | Le contrat impose `409 relecture déjà rendue` ; cohérence du modèle (aucun chemin de réécriture en base) ; verbatim du client (« c'est plus honnête pour tout le monde ») — détail en CDC §7.1 |
| Éligibilité au tirage = **toute présence** (y compris `FORMATEUR`) | CDC §7.2 |
| Tirage **immédiat au dépôt**, même transaction | CDC §7.3 |
| Exercice sans candidat → `SANS_RELECTEUR`, déblocage **manuel** par le formateur (qui ne relit jamais lui-même) | CDC §7.4 |
| La **clôture** ferme dépôts et présences, **pas** les relectures assignées | CDC §7.5, RG21 |
| Code unique parmi **toutes** les sessions (closes incluses) | RG1 : « saisir le code » doit rester non ambigu |

## 7. État d'avancement (par rapport aux 5 étapes)

- **Étape 1 — analyse** : **livrée** (jalon `[JALON] analyse` en tête d'historique) : CDC, D1–D4, contrat, 14 issues créées puis toutes fermées par leurs fusions.
- **Étape 2 — construction** : **livrée** (jalon `[JALON] v0.1`) : les 14 issues (Must et Should) implémentées et fusionnées, frontend complet, base de démonstration H2, 143 tests verts à la livraison.
- **Étape 3 — enveloppe (bug + changement de besoin)** : **livrée** — l'enveloppe n'ayant jamais pu être collée, son contenu a été reconstruit localement par audit du code et consigné avec sa provenance (`docs/enveloppes/enveloppe-etape3.md`) ; bug #15 corrigé par **test rouge d'abord** (PR #17), évolution #16 livrée en 6 commits (PR #18) : migration V3, champ `exercicesSansRelecteur`, écran de désignation, trace RG23, contrat, repriorisation et analyse mises à jour ; 149 tests verts.
- **Étape 4 — finalisation** : **livrée** — README réécrit et **testé depuis un clone vierge réel** (clone, démarrage, seed vérifié par API, build frontend), `CHANGELOG.md` aligné sur la chronologie réelle, tri du backlog (18/18 issues fermées, 0 ouverte, aucune obsolète), vérifications d'historique (aucun artefact ni secret, identité vérifiée), jalon `[JALON] v1.0` posé en dernier commit.
