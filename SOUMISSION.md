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
5. Le **formateur** voit le **tableau** de la promotion : présences (dont ajoutées à la main, Q14), exercices déposés, **moyenne des notes reçues**, relectures en attente (Q16) — et peut **clôturer** la session (dépôts et présences fermés, relectures assignées restées rendables, RG21).

## 2. Démarrage en 3 commandes (clone vierge, rien à installer comme SGBD)

```bash
cd backend && ./mvnw spring-boot:run    # 1. API sur http://localhost:8080
cd frontend && npm install              # 2. dépendances du frontend (une seule fois)
cd frontend && npm run dev              # 3. interface sur http://localhost:5173
```

- **Base de données** : H2 en **fichier persistant** `backend/data/kfokam48` (mode PostgreSQL), créée par **Flyway** au premier démarrage avec les **données de démonstration** (2 promotions, 16 étudiants) — l'application n'est jamais vide. Supprimer le dossier `data/` pour repartir de zéro.
- **PostgreSQL optionnel** : surcharger `KFOKAM48_DB_URL`, `KFOKAM48_DB_USER`, `KFOKAM48_DB_PASSWORD` (aucun secret committé).
- **Vérification complète** (tests unitaires + intégration, 123 tests, H2 en mémoire — aucun serveur requis) :

```bash
cd backend && ./mvnw verify
cd frontend && npm run build
```

## 3. Arborescence du dépôt

| Chemin | Contenu |
|---|---|
| `docs/CAHIER_DES_CHARGES.md` | Analyse complète : 10 sections imposées, EF1–EF15, RG1–RG22, arbitrages §7 (dont **Q10/Q15**) |
| `docs/diagrammes/` | D1 cas d'utilisation · D2 modèle de données (normatif Flyway) · D3 séquence « marquer sa présence » · D4 cycle de vie d'un exercice (bonus) |
| `docs/JOURNAL.md` | Journal de bord : fait / blocage / vérification, entrée par issue |
| `docs/issues/` | Backlog détaillé (les **issues GitHub #1–#14** reprennent ce contenu) |
| `api/contrat.yaml` | **Contrat d'API fait foi** : 12 chemins, 13 opérations, les 5 opérations imposées incluses, format d'erreur `{code, message}` partout |
| `backend/` | Spring Boot 3.2, Java 17, `mvnw` commité, Flyway (V1 schéma, V2 seed), contrôleur/service/repository stricts, DTO obligatoires, `@RestControllerAdvice` |
| `frontend/` | React + TypeScript (Vite) : 3 écrans requis + liste des étudiants ; **couche API unique** (`src/api/`), moyenne **jamais recalculée** côté client |
| `CLIENT.md` | Besoin brut du client (16 Q/R) conservé verbatim |
| `SOUMISSION.md` | Ce fichier |

## 4. Tests (le sujet en exige deux ; le projet en compte 123)

- **Unitaires** (43) : règles métier réelles — expiration du code, unicité de présence, tirage au sort (jamais l'auteur), validation de lien, blocage après 5 échecs…
- **Intégration** (80) : un fichier IT par endpoint, sur **H2 en mémoire** avec les vraies migrations Flyway — poste vierge, aucune base locale pré-existante.
- Exemple de vérification de bout en bout : `410 CODE_EXPIRE` prioritaire sur `409 DEJA_PRESENT` (D3), `409 RELECTURE_DEJA_RENDUE` avec note d'origine inchangée (Q15).

## 5. Organisation Git exigée par le sujet

- **Identité** : tous les commits signés `Candidat-KF48-YAO-249 <kf48-yao-249@kfokam48.local>` — aucun nom réel nulle part (commits, issues, fichiers).
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

- **Étape 1 — analyse** : livrée et poussée (jalon `[JALON] analyse` en tête d'historique).
- **Étape 2 — construction des Must** : 8 issues fusionnées (#1–#5, #7, #8 + #4), issues #9, #10, #12, #13 implémentées avec 123 tests verts et poussées sur leurs branches ; écran relecteur livré ; base de démonstration **H2** (démarrage vérifié par appels API réels) ; le jalon `[JALON] v0.1` est posé après fusion des dernières branches et fusion des issues Should restantes (#6, #11, #14).
- **Étape 3 — enveloppe (bug + changement de besoin)** : en attente du contenu exact de l'enveloppe (texte du bug et du changement).
- **Étapes 4–5** : à venir (CHANGELOG, checklist, jalon `v1.0`, relecture de bout en bout).
