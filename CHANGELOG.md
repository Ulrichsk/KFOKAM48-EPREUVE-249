# CHANGELOG

Toutes les évolutions livrées de KFOKAM48, dans l'ordre chronologique réel des commits
et jalons de `main`. Ce fichier ne liste que ce qui a réellement été fait.

Les dates sont celles des commits : toute l'étape 2 est tombée le 2026-09-25, l'étape 3
le 2026-09-27.

## [v1.0] — 2026-09-27 (jalon `[JALON] v1.0`)

### Finalisation (étape 4)

- **README** réécrit et **testé depuis un clone vierge réel** (nouveau dossier, `git
  clone`, commandes documentées exécutées) : prérequis explicites (Java 17, Node 18+),
  confirmation du chargement automatique des données de démonstration (vérifié par
  appels API : API prête, présence 201, tableau à jour), ajout du « pourquoi React »,
  suppression des doublons, structure à jour (V3, CHANGELOG).
- **CHANGELOG** créé (ce fichier), aligné sur l'historique Git réel.
- **Tri du backlog** : aucune issue obsolète à fermer (les Should #4, #6, #11 sont
  toutes livrées) ; les évolutions éventuelles restantes sont explicitées dans
  `docs/REPRIORISATION_BACKLOG.md`.
- Journal complété (`docs/JOURNAL.md`), vérifications d'historique (aucun `target/`,
  `node_modules/`, `dist/`, secret ou identité réelle dans les commits de code).
- Décision assumée : les deux commits de fusion de l'étape 3, créés via l'API GitHub,
  portent l'identité du compte de dépôt plutôt que l'identité candidat — correction
  refusée car elle exigerait un `push --force` sur `main`, interdit par la convention
  du projet.

## [Étape 3] — 2026-09-27 · enveloppe : bug + changement de besoin

L'enveloppe du surveillant n'ayant jamais pu être collée (placeholders vides), son
contenu a été reconstruit localement par audit du code, consigné et justifié dans
`docs/enveloppes/enveloppe-etape3.md`.

### Corrigé

- **#15** — `PUT /api/exercices/{id}` répondait `400 CHAMP_MANQUANT` pour un lien vide,
  en contradiction avec le contrat (qui ne promet qu'un seul 400 : `LIEN_INVALIDE`) et
  avec la décision de conception du dépôt (validation dans le service). Reproduit par
  **test rouge d'abord** (`RemplacementLienIT.repond_lien_invalide_pour_un_lien_vide`),
  puis corrigé en retirant `@NotBlank` de `DemandeRemplacementLien`. Fusionné par la
  PR #17 seule (jamais mélangée à l'évolution).

### Ajouté (changement de besoin)

- **#16** — le formateur désigne un relecteur aux exercices restés `SANS_RELECTEUR`
  **directement depuis l'écran du tableau** (la voie de secours RG10 devient opérante ;
  le tirage au sort RG9 reste la voie normale) :
  - migration **V3** `audits_assignation_manuelle` — **V1 et V2 strictement
    intouchées** (diff vérifié) — et trace d'audit **RG23** écrite dans la transaction
    de chaque assignation manuelle réussie ;
  - `LigneTableau.exercicesSansRelecteur` (champ additif) servi par une seconde requête
    agrégée assemblée en mémoire (Hibernate refuse une sous-requête multi-lignes dans
    `SELECT new` ; ENF8 préservé) ;
  - frontend : colonne « Exercices sans relecteur », sélection du relecteur autre que
    l'auteur, messages 403/409 de l'API affichés tels quels, rechargement après succès.
  Fusionné par la PR #18 seule, en six commits (V3, backend, contrat, frontend,
  repriorisation, analyse).
- **Contrat** (`api/contrat.yaml`) : champ additif documenté, usage écran et RG23.
- **Analyse à jour** (point 7 du protocole) : CDC (RG18 complétée, **RG23 créée**,
  §7.4 complété), D2 en 1.1 (huitième table), D4 en 1.2 (transition annotée) — commit
  explicitement intitulé « Mise à jour de l'analyse suite au changement de besoin
  (enveloppe étape 3) ».
- **Repriorisation documentée** (point 6) : `docs/REPRIORISATION_BACKLOG.md` — aucune
  issue livrée ne devient obsolète, les Should restent Should.
- Tests : **149 verts** (43 unitaires + 106 IT), dont nouvelle IT
  `DesignationDepuisTableauIT` (parcours client complet, refus 403 auteur et
  hors-promotion, présence/absence de la trace).

## [v0.1] — 2026-09-25 (jalon `[JALON] v0.1`)

### Livré (étapes 1 et 2) — issues #1 à #14, toutes fermées par leurs fusions

- **Analyse (étape 1, jalon `[JALON] analyse` en tête d'historique)** :
  `docs/CAHIER_DES_CHARGES.md` (10 sections imposées), diagrammes D1–D4,
  `docs/issues/` (14 issues), `api/contrat.yaml` (12 chemins / 13 opérations, les 5
  imposées incluses), `CLIENT.md` conservé verbatim.
- **Socle** (#1) : Flyway V1 (7 tables) et V2 (référentiel : 2 promotions, 16
  étudiants), gestion d'erreurs globale au format `{code, message}`, `GET
  /api/promotions/{id}/etudiants`.
- **Parcours formateur** : ouvrir une session avec code valable 15 min (#2), ajouter
  une présence à la main (#14), tableau de bord (une ligne par étudiant, moyenne
  calculée par l'API jamais recalculée côté client, #12), clôturer la session
  (idempotent, #13).
- **Parcours étudiant** : choisir son nom dans une liste et marquer sa présence par
  code (#3), déposer le lien de son exercice jusqu'à clôture (#5), remplacer son lien
  tant que la relecture n'a pas commencé (#6), consulter sa note et son commentaire
  sans connaître le relecteur (#11).
- **Relecture entre pairs** : tirage au sort parmi les présents, jamais l'auteur (#7),
  débloquer par désignation manuelle un exercice resté sans relecteur (#8), consulter
  le lien à relire en anonymat (première consultation horodatée, #9), rendre une note
  entière sur 20 avec commentaire **définitive dès l'envoi** (#10), blocage après
  cinq codes erronés (#4).
- **Frontend** React + TypeScript : les trois parcours requis, le tableau de bord et
  la liste des étudiants ; couche API unique ; la moyenne n'est jamais recalculée côté
  client.
- **Base de démonstration** : développement basculé sur H2 en fichier persistant
  (aucun SGBD à installer), démarrage vérifié par appels API réels.
- Tests à la livraison : 143 verts (43 unitaires + 100 IT).
