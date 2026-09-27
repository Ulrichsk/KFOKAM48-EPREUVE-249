# Enveloppe de l'étape 3 — contenu reconstruit localement

> **⚠️ Provenance — à lire avant d'évaluer.**
> L'enveloppe physique du surveillant n'a jamais pu être collée dans la conversation
> (les trois envois ne contenaient que les placeholders vides
> `[colle ici le bug exact décrit dans l'enveloppe]` et
> `[colle ici le changement exact décrit dans l'enveloppe]`).
> Sur instruction du candidat, le contenu ci-dessous a été **reconstruit localement**
> à partir d'un audit complet du code livré à l'étape 2. Il est présenté comme tel :
> rien ne prétend provenir du surveillant, chaque élément est **objectivement
> vérifiable dans le dépôt** (scénario de reproduction fourni). Si l'enveloppe
> originale parvient ultérieurement, ce fichier sera remplacé et le travail réajusté.

---

## 1. BUG SIGNALÉ PAR LE CLIENT

### Énoncé (reconstitué)

> « J'ai voulu corriger mon lien d'exercice après m'être trompé de fichier. Mon client
> API m'a renvoyé `[CHAMP_MANQUANT] Le champ « lien » est obligatoire ou invalide.`
> alors que votre contrat promet `400 LIEN_INVALIDE` pour tout lien non valide —
> or un lien vide n'est pas une URL valide. Vos messages d'erreur ne correspondent pas
> à votre propre documentation : je ne sais plus quoi croire. »

### Analyse — vérifié dans le code

- Le contrat (`api/contrat.yaml`, § `/api/exercices/{id}`, opération `remplacerLienExercice`)
  ne documente qu'**une seule** réponse 400 : la référence `LienInvalide`, définie comme
  « `400 LIEN_INVALIDE` — le lien n'est pas une URL absolue `http` ou `https` de 500
  caractères au plus ». Un lien vide fait donc partie des cas couverts, et la seule
  réponse promise est `LIEN_INVALIDE`.
- L'implémentation contredit le contrat : `DemandeRemplacementLien` porte
  `@NotBlank` sur `lien`. Ce lien vide est intercepté **avant le service** par la
  validation de bean, le `GestionnaireErreurs` traduit la
  `MethodArgumentNotValidException` en `400 {"code": "CHAMP_MANQUANT"}` — code
  jamais promis par le contrat pour cette opération.
- Cette divergence contredit aussi la décision de conception documentée du projet :
  la Javadoc de `DemandeDepotExercice` pose que « le contrat attend `400
  LIEN_INVALIDE` pour un lien vide, relatif ou mal formé, et non `CHAMP_MANQUANT`.
  C'est donc le service qui le valide (`ValidateurLien`), avec un seul code d'erreur
  pour tous les cas ». Le dépôt suit cette règle (lien vide → `LIEN_INVALIDE`) ; le
  remplacement, ajouté plus tard (issue 06), a dérivé sans que personne ne s'en
  aperçoive — aucun test d'intégration ne couvrait le lien vide sur `PUT`.
- Le service, lui, était prêt : `remplacerLien` transforme déjà un lien `null` en
  chaîne vide et le confie au validateur (→ `LIEN_INVALIDE`). C'est l'annotation
  de couche HTTP qui court-circuite ce chemin.

### Scénario de reproduction

1. Démarrer le backend (`cd backend && ./mvnw spring-boot:run`).
2. Ouvrir une session, y déposer un exercice (soit par l'API, soit via l'écran).
3. Tenter de remplacer le lien par une chaîne vide :
   `curl -X PUT http://localhost:8080/api/exercices/{id} -H "X-Etudiant-Id: {auteur}" -H "Content-Type: application/json" -d '{"lien": ""}'`
4. **Attendu** (contrat) : `400 {"code": "LIEN_INVALIDE", "message": "Le lien doit être
   une URL absolue…"}`.
5. **Observé** : `400 {"code": "CHAMP_MANQUANT", "message": "Le champ « lien » est
   obligatoire ou invalide."}`.

### Correctif attendu

La validation du lien doit revenir là où la décision de conception l'a placée : dans
le service, avec un seul code d'erreur. Concrètement : supprimer `@NotBlank` de
`DemandeRemplacementLien` (alignement sur `DemandeDepotExercice`), de sorte que tout
lien vide ou mal formé traverse la couche HTTP et reçoive la réponse contractuelle
`400 LIEN_INVALIDE`. Un test d'intégration doit d'abord démontrer le bug (rouge),
alors qu'aucun test existant ne couvre ce cas.

---

## 2. CHANGEMENT DE BESOIN

### Énoncé (reconstitué)

> « Le tableau de bord affiche déjà les exercices restés **sans relecteur**, mais
> aujourd'hui je dois appeler l'API à la main pour en désigner un. Je veux désigner
> directement depuis l'écran : sur la ligne de l'étudiant concerné, je choisis un
> relecteur parmi les autres étudiants **présents** de la promotion et je valide.
> Tout le reste ne change pas — le tirage au sort automatique au dépôt reste la voie
> normale. »

### Analyse — vérifié dans le code

- L'endpoint existe déjà et est conforme au contrat : `POST /api/exercices/{id}/relecteur`
  (`409 RELECTURE_DEJA_ASSIGNEE`, `403 AUTO_EVALUATION_INTERDITE` si l'auteur est
  proposé, `403 ACCES_REFUSE` si le relecteur proposé n'est pas de la promotion,
  `404`…). Il n'a **aucun écran** : seul le formateur peut l'appeler, à la main.
- Le tableau (`GET /api/promotions/{id}/tableau`, `TableauService`) agrège
  `presences`, `presencesFormateur`, `exercicesDeposes`, `moyenne`,
  `relecturesEnAttente` par étudiant — mais **ne dit pas** quels exercices sont restés
  `SANS_RELECTEUR`, ni pour qui : le formateur ne peut pas savoir sur quelle ligne
  intervenir sans aller interroger la base.
- La liste des étudiants d'une promotion existe (`GET /api/promotions/{id}/etudiants`).

Le changement de besoin exige donc une **évolution** et non un simple habillage :

1. Le tableau doit exposer les exercices restés `SANS_RELECTEUR` de chaque étudiant
   (identifiants), pour que le formateur sache où agir ;
2. L'écran tableau doit offrir, sur la ligne concernée, la sélection d'un relecteur
   parmi les étudiants de la promotion (l'exclusion de l'auteur et des hors-promotion
   reste **garantie par l'API**, l'écran ne duplique aucune règle) ;
3. Après assignation, le tableau est rechargé : l'exercice passe `EN_ATTENTE` et sort
   de la liste des sans-relecteur.

L'enveloppe ne précise pas la liste autorisée : le tableau propose tous les
étudiants de la promotion **autre que l'auteur de la ligne** (rapprochement de Q5),
et laisse l'API trancher le reste — l'écran ne duplique aucune règle de gestion.

### Impact sur le schéma de base

**Aucun changement de structure n'est nécessaire** : `exercices.statut` porte déjà
`SANS_RELECTEUR` et `relectures` porte déjà `assigne_par = FORMATEUR`.

**Décision persistée néanmoins (traçabilité de l'enveloppe)** : pour garder une trace
fiable de qui a agi au nom du formateur et quand — aujourd'hui implicite via
`relectures.assigne_par` et `assigne_at` — une table d'audit des assignations manuelles
est ajoutée par la migration **`V3__audit_assignation_manuelle.sql`** (`id`, `exercice_id`,
`relecteur_id`, `assigne_par_id`, `assigne_at`). Elle est écrite à chaque assignation
manuelle réussie et ne change aucun comportement métier. C'est la seule évolution de
schéma, dans une **nouvelle** migration (V1 et V2 ne sont jamais retouchées).

### Impact sur le contrat `api/contrat.yaml`

- `GET /api/promotions/{id}/tableau` : le schéma `LigneTableau` gagne un champ
  `exercicesSansRelecteur` (tableau d'identifiants d'exercices, vide le plus souvent).
  Champ **additif**, compatible.
- `POST /api/exercices/{id}/relecteur` : inchangé (déjà conforme).
- Aucune autre route ni aucun code d'erreur ne change.

### Impact sur le cahier des charges

- **RG18** (tableau) : complétée — la ligne d'un étudiant expose aussi ses exercices
  restés sans relecteur.
- **RG10** : complétée — le déblocage manuel est opérant **depuis l'écran** du tableau
  (avant : uniquement via l'API).
- **RG23** (nouvelle) : chaque assignation manuelle est tracée dans la table
  `audits_assignation_manuelle` (qui, pour quel relecteur, par qui, quand).
- Diagramme **D2** (données) : ajout de la table d'audit V3.
- Diagramme **D3** (séquence « marquer présence ») : non concerné — le bug corrigé est
  un défaut d'affichage frontend, aucune règle de gestion ni séquence backend ne change.
- Diagramme **D4** (états d'un exercice) : une transition précisée
  `SANS_RELECTEUR → EN_ATTENTE` déclenchée depuis l'écran tableau (avant : API seule).

---

## 3. RAPPEL DU PROTOCOLE À APPLIQUER (rappelé par le surveillant)

1. Une issue GitHub **dédiée au bug**, une issue **dédiée au changement** — jamais
   mélangées, ni dans la même issue ni dans le même commit.
2. Reproduire le bug par un **test qui échoue d'abord**.
3. Évolution de schéma uniquement par une **NOUVELLE migration Flyway** (V3).
4. Mettre à jour `api/contrat.yaml`.
5. Mettre à jour le frontend.
6. **Reprioriser explicitement le backlog restant** et documenter cette repriorisation.
7. Mettre à jour `docs/CAHIER_DES_CHARGES.md` et les diagrammes dans un commit dont le
   message dit explicitement que c'est une **mise à jour de l'analyse suite au
   changement de besoin (enveloppe étape 3)**.
8. Correctif et évolution fusionnés via des **PR séparées**, chacune rattachée à son
   issue, chaque action = un commit séparé et explicite, poussé immédiatement.
