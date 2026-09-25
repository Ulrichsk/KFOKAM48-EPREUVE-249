# Cahier des charges — KFOKAM48

| | |
|---|---|
| Projet | KFOKAM48 — présence, dépôt d'exercices et relecture entre pairs |
| Version | 1.0 (issue de l'étape 1 — ANALYSE) |
| Source | `CLIENT.md` (besoin client + ANNEXE A : 16 Q/R + ANNEXE B : contrat d'API imposé) |
| Contrat | `api/contrat.yaml` — fait foi pour les chemins, verbes et codes HTTP |

---

## 1. Contexte et objectif

Un formateur anime des sessions de cours pour une promotion d'étudiants. Aujourd'hui, le suivi
de la présence et des exercices se fait à la main : codes dictés à l'oral, liens envoyés par
messages, notes reportées dans un tableur. Trois conséquences : l'appel prend du temps de cours,
un même étudiant peut être compté deux fois, et la relecture entre pairs n'est ni systématique
ni anonyme.

**Objectif** : fournir une application web qui permet au formateur d'ouvrir une session et
d'obtenir un code de présence, aux étudiants de marquer leur présence et de déposer le lien de
leur exercice, au système d'assigner automatiquement un relecteur à chaque exercice, et au
formateur de consulter un tableau unique par promotion regroupant présences, exercices déposés,
moyenne des notes reçues et relectures encore attendues.

**Bénéfice attendu** : l'appel est instantané et fiable, chaque exercice est relu par un pair
désigné au hasard, et le formateur voit en une seule vue ce qui reste en attente.

**Périmètre fonctionnel minimal** — les 5 opérations du contrat imposé (`CLIENT.md` §3) sont
livrées telles quelles ; les opérations complémentaires nécessaires aux Q/R (clôture de session,
ajout manuel de présence, consultation du lien à relire, consultation de sa note, assignation
manuelle d'un relecteur) sont ajoutées à `api/contrat.yaml` et listées en section 4.

---

## 2. Acteurs et rôles

| Acteur | Qui c'est | Ce qu'il fait |
|---|---|---|
| **Formateur** | Enseignant responsable de la promotion | Ouvre une session et diffuse le code, ajoute une présence à la main (Q14), assigne un relecteur quand le tirage n'a trouvé personne, clôture la session, consulte le tableau de bord (Q16) |
| **Étudiant** | Membre d'une promotion | Choisit son nom dans la liste de sa promotion (Q1, aucun mot de passe), marque sa présence avec le code, dépose le lien de son exercice, remplace ce lien tant que la relecture n'a pas commencé (Q13), consulte la note et le commentaire reçus (Q8) |
| **Relecteur** | Un **étudiant** désigné par tirage au sort | Rôle et non acteur distinct : il consulte le lien de l'exercice d'un pair, envoie une note entière sur 20 et un commentaire |

### 2.1 Absence d'authentification — hypothèse structurante

Q1 est explicite : « Non, l'étudiant choisit son nom dans une liste. Ne perdez pas de temps
là-dessus. » **Il n'y a ni compte, ni mot de passe, ni session utilisateur côté serveur.**
Le client envoie `etudiantId` ou `formateurId` dans le corps ou l'URL ; le serveur fait
confiance à cette identité. C'est une conséquence assumée de Q1, pas un oubli : elle est
documentée en section 7 et en section 5 (ENF7). Les garde-fous métier qui ne dépendent pas de
l'identité (unicité de présence, unicité d'exercice, note définitive, autoévaluation interdite)
restent appliqués côté serveur, car ils protègent contre les erreurs et les doubles clics
autant que contre la malveillance.

---

## 3. Périmètre (inclus / exclu)

### 3.1 Inclus

- Création d'une session de cours rattachée à une promotion, avec génération d'un code de présence.
- Marquage de présence par code, avec expiration à 15 minutes (Q2) et unicité par session (RG4).
- Ajout manuel d'une présence par le formateur, tracé par la source `FORMATEUR` (Q14).
- Dépôt du lien d'un exercice pour une session, et remplacement du lien tant qu'aucune relecture
  n'a commencé (Q13).
- Tirage au sort d'un seul relecteur par exercice, parmi les étudiants présents à la session,
  à l'exclusion de l'auteur (Q5, Q6, Q7).
- Relecture : consultation du lien, envoi d'une note entière de 0 à 20 et d'un commentaire (Q8, Q9).
- Tableau de bord par promotion : présences, exercices déposés, moyenne des notes reçues,
  relectures en attente (Q16), avec mise en évidence des exercices jamais relus (Q11).
- Clôture de la session par le formateur (Q10, Q11, Q12).
- Limitation des tentatives de saisie du code : blocage de 2 minutes après 5 échecs (Q4).
- Jeu de données de référence : promotions et étudiants (nécessaire pour Q1 et pour les
  erreurs `404`/`400` du contrat).

### 3.2 Exclu

- Authentification, comptes, mots de passe, rôles persistés (Q1) — toute sécurité d'identité est hors sujet.
- Messagerie, notifications par e-mail ou SMS, envoi du code autrement que par affichage à l'écran.
- Gestion des notes hors relecture entre pairs : pas de notation finale par le formateur,
  pas de pondération, pas de bulletin.
- Rendu de fichiers : les étudiants déposent un **lien**, pas un fichier.
- Multi-tenant, plusieurs formateurs par promotion, gestion administrative des promotions.
- Modification d'une note après envoi, par qui que ce soit (Q15, décision section 7).
- Application mobile native ; l'interface est web responsive.
- Internationalisation, thème sombre, accessibilité avancée (hors périmètre du jury).

---

## 4. Exigences fonctionnelles

Chaque exigence est vérifiable par un critère « quand … alors … ». Les codes d'erreur cités
correspondent exactement à ceux de `api/contrat.yaml`.

### EF1 — Ouvrir une session et obtenir un code de présence

Le formateur crée une session rattachée à une promotion et reçoit un code à dicter.
**Critère d'acceptation** : quand le formateur envoie `POST /api/sessions` avec `titre` et
`promotionId` valides, alors la réponse est `201` avec `id`, `code`, `ouvertureAt` et
`expirationAt` tel que `expirationAt = ouvertureAt + 15 minutes` ; quand un champ est absent ou
vide, alors la réponse est `400 { "code": "CHAMP_MANQUANT" }` ; quand `promotionId` ne désigne
aucune promotion, alors la réponse est `404 { "code": "PROMOTION_INCONNUE" }`.

### EF2 — Marquer sa présence avec le code

**Critère d'acceptation** : quand un étudiant envoie `POST /api/presences` avec le `code` exact
d'une session non clôturée et son `etudiantId`, alors la réponse est `201` avec `id`,
`sessionId`, `etudiantId` et `source = "ETUDIANT"` ; quand le code est valide mais que
l'étudiant est déjà présent à cette session, alors la réponse est `409 { "code": "DEJA_PRESENT" }`
sans créer de seconde ligne.

### EF3 — Refuser une présence hors délai

**Critère d'acceptation** : quand un étudiant envoie un code dont l'expiration est dépassée,
alors la réponse est `410 { "code": "CODE_EXPIRE" }` ; quand le code est syntaxiquement valide
mais n'existe pas, alors la réponse est `400 { "code": "CODE_INCONNU" }` ; quand la session a
été clôturée par le formateur, alors la réponse est `409 { "code": "SESSION_CLOTUREE" }`.

### EF4 — Ajouter une présence à la main

Couvre Q14 : un étudiant sans téléphone fonctionnel doit pouvoir être enregistré.
**Critère d'acceptation** : quand le formateur envoie `POST /api/sessions/{id}/presences` avec un
`etudiantId` de la promotion concernée, alors la réponse est `201` avec `source = "FORMATEUR"`
et le tableau de bord affiche cette présence comme ajoutée par le formateur.

### EF5 — Déposer le lien d'un exercice

**Critère d'acceptation** : quand un étudiant envoie `POST /api/exercices` avec `sessionId`,
`etudiantId` et un `lien` valide, alors la réponse est `201` avec `id` et un `statut` reflétant
l'issue du tirage (`EN_ATTENTE` si un relecteur a été trouvé, `SANS_RELECTEUR` sinon) ; quand le
lien est vide ou mal formé, alors la réponse est `400 { "code": "LIEN_INVALIDE" }` ; quand
l'étudiant a déjà déposé un exercice pour cette session, alors la réponse est
`409 { "code": "EXERCICE_DEJA_DEPOSE" }`.

### EF6 — Remplacer le lien d'un exercice non encore entamé

Couvre Q13.
**Critère d'acceptation** : quand l'auteur remplace son lien via `PUT /api/exercices/{id}` et
qu'aucun relecteur n'a encore ouvert le lien, alors la réponse est `200` et le nouveau lien est
le seul consultable ; quand un relecteur a déjà ouvert le lien, alors la réponse est
`409 { "code": "RELECTURE_COMMENCEE" }` et l'ancien lien reste en place.

### EF7 — Assigner automatiquement un relecteur par tirage au sort

Couvre Q6 et Q7 : un seul relecteur, tiré au hasard parmi les présents.
**Critère d'acceptation** : quand un exercice est déposé et qu'au moins deux étudiants présents
à la session autres que l'auteur existent, alors exactement un relecteur est désigné,
différent de l'auteur, choisi parmi les étudiants ayant une présence à cette session quelle
que soit la source de la présence ; quand un second dépôt a lieu, alors il reçoit un relecteur
indépendamment du premier.

### EF8 — Ne jamais laisser un exercice orphelin sans le signaler

Couvre Q11 et le cas limite tranché en section 7.
**Critère d'acceptation** : quand aucun étudiant éligible n'existe (aucune présence, ou le seul
candidat est l'auteur), alors l'exercice passe au statut `SANS_RELECTEUR`, la réponse de dépôt
le signale, et l'exercice apparaît dans le tableau du formateur comme relecture en attente ;
quand le formateur désigne un relecteur via `POST /api/exercices/{id}/relecteur`, alors le
statut devient `EN_ATTENTE` ; quand le `relecteurId` proposé est l'auteur, alors la réponse est
`403 { "code": "AUTO_EVALUATION_INTERDITE" }`.

### EF9 — Consulter le lien de l'exercice à relire

**Critère d'acceptation** : quand le relecteur désigné ouvre `GET /api/relectures/{id}`, alors la
réponse est `200` avec le lien, la session et le statut, **sans** le nom de l'auteur ni celui du
relecteur ; quand un autre étudiant que le relecteur désigné demande la même ressource, alors la
réponse est `403 { "code": "ACCES_REFUSE" }` ; quand cette consultation a lieu, alors la date de
première consultation est enregistrée (elle conditionne EF6).

### EF10 — Rendre sa relecture

Couvre Q9 et Q15.
**Critère d'acceptation** : quand le relecteur désigné envoie `POST /api/relectures/{id}` avec une
note entière comprise entre 0 et 20, alors la réponse est `200` et la relecture passe au statut
`RENDUE` ; quand la note est décimale, négative ou supérieure à 20, alors la réponse est
`400 { "code": "NOTE_INVALIDE" }` ; quand la relecture a déjà été rendue, alors la réponse est
`409 { "code": "RELECTURE_DEJA_RENDUE" }` et la note reste inchangée ; quand l'appelant est
l'auteur de l'exercice, alors la réponse est `403 { "code": "AUTO_EVALUATION_INTERDITE" }`.

### EF11 — Consulter la note et le commentaire reçus

Couvre Q8.
**Critère d'acceptation** : quand l'auteur d'un exercice relu consulte
`GET /api/etudiants/{id}/exercices`, alors il voit la note et le commentaire, mais aucune
information permettant d'identifier le relecteur.

### EF12 — Afficher le tableau de bord d'une promotion

Couvre Q16.
**Critère d'acceptation** : quand le formateur appelle `GET /api/tableau?promotionId=` avec une
promotion connue, alors la réponse est `200` avec une ligne par étudiant de la promotion
comprenant `nom`, nombre de présences, nombre d'exercices déposés, moyenne des notes reçues et
nombre de relectures en attente ; quand un étudiant n'a ni note ni exercice, alors sa ligne est
présente avec des valeurs à zéro ou nulles plutôt qu'absente ; quand la promotion est inconnue,
alors la réponse est `404 { "code": "PROMOTION_INCONNUE" }`.

### EF13 — Limiter la devinette du code

Couvre Q4.
**Critère d'acceptation** : quand un étudiant enchaîne 5 tentatives de code infructueuses, alors
toute tentative suivante dans les 2 minutes reçoit `429 { "code": "TROP_DE_TENTATIVES" }` ;
quand le délai est écoulé, alors les tentatives sont à nouveau possibles et le compteur est remis
à zéro après une réussite.

### EF14 — Clôturer la session

Couvre Q10, Q11 et Q12.
**Critère d'acceptation** : quand le formateur envoie `POST /api/sessions/{id}/cloture`, alors la
réponse est `200` avec `clotureAt` ; après cette clôture, tout `POST /api/presences`,
`POST /api/exercices` ou `PUT /api/exercices/{id}` sur cette session échoue avec
`409 { "code": "SESSION_CLOTUREE" }`, tandis que les relectures déjà assignées restent rendables.

### EF15 — Choisir son nom dans la liste de la promotion

Couvre Q1.
**Critère d'acceptation** : quand l'application appelle `GET /api/promotions/{id}/etudiants`,
alors la réponse est `200` avec la liste des étudiants de la promotion triée par nom, et chaque
entrée expose au minimum `id`, `nom` et `prenom`.

---

## 5. Exigences non fonctionnelles

- **ENF1 — Pile imposée.** Backend Java 17, Spring Boot et Maven, avec le wrapper `mvnw` **commité**
  et fonctionnel (`./mvnw verify` doit marcher sur un poste vierge, sans Maven installé).
- **ENF2 — Conformité au contrat.** Les chemins, verbes et codes HTTP de `api/contrat.yaml` sont
  respectés à la lettre. Toute opération ajoutée y est documentée dans le même style.
- **ENF3 — Séparation des couches.** Contrôleur HTTP sans aucune requête base ; toute la logique
  métier dans les services ; persistance dans les repositories. Aucune entité JPA ne traverse
  l'API : uniquement des DTO.
- **ENF4 — Gestion d'erreurs centralisée.** Un `@RestControllerAdvice` unique convertit toute
  exception en `{ "code": "CODE_ERREUR", "message": "..." }`. Aucune stack trace, aucun nom de
  classe interne, aucun message SQL ne peut atteindre le client. Une exception non prévue devient
  un `500 { "code": "ERREUR_INTERNE" }` avec un message générique.
- **ENF5 — Schéma versionné.** Toutes les tables sont créées par Flyway dans
  `backend/src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto=validate` partout,
  y compris en profil de test ; `update` est interdit.
- **ENF6 — Traçabilité.** Les horodatages sont stockés et échangés en UTC, au format ISO-8601.
  Les événements métier structurants (dépôt, tirage, rendu, clôture) sont datés en base.
- **ENF7 — Pas de sécurité d'identité, mais des garde-fous métier.** Conformément à Q1, aucune
  authentification. Les invariants métier sont néanmoins vérifiés côté serveur (unicité de
  présence, unicité d'exercice, note définitive, autoévaluation interdite, relecture assignée à
  un seul relecteur).
- **ENF8 — Performance du tableau.** Le tableau d'une promotion est servi par un nombre constant
  de requêtes agrégées, sans boucle N+1 par étudiant.
- **ENF9 — Testabilité.** Chaque règle de gestion de la section 6 possède au moins un test
  automatisé la couvrant en succès et en échec ; les cas d'erreur du contrat sont testés au
  niveau HTTP.
- **ENF10 — Frontend React.** Interface React consommant l'API, avec trois parcours : formateur
  (session, présence manuelle, tableau, clôture), étudiant (présence, dépôt, consultation de sa
  note), relecteur (liste de ses relectures, consultation du lien, saisie de la note).
- **ENF11 — Hygiène du dépôt.** `.gitignore` Java et JavaScript posé avant le premier commit de
  code (`target/`, `node_modules/`, `build/`, `dist/`, `.env`, etc.). Aucun secret, mot de passe
  ou jeton dans l'historique.
- **ENF12 — Lisibilité.** Code et documentation en français pour les libellés métier, nommage
  technique en anglais. Le présent cahier des charges et `api/contrat.yaml` restent la référence
  en cas de doute.

---

## 6. Règles de gestion

| # | Règle | Origine |
|---|---|---|
| **RG1** | Le code de présence est aléatoire (`SecureRandom`), long de 6 caractères pris dans un alphabet restreint excluant `0`, `O`, `1`, `I` et `L`, et **unique toutes sessions confondues** (contrainte en base). | ANNEXE B / hypothèse H2 ; l'unicité globale est précisée à l'étape 2 : un code déjà utilisé ne doit plus désigner aucune session, sinon la résolution du code par l'étudiant serait ambiguë. |
| **RG2** | Le code expire 15 minutes après l'ouverture de la session : `expirationAt = ouvertureAt + 15 min`. | Q2 |
| **RG3** | Une présence ne peut être marquée que pendant la validité du code et tant que la session n'est pas clôturée. | Q2, Q3 |
| **RG4** | Un étudiant n'a au plus qu'une présence par session : l'unicité est garantie par contrainte en base, pas seulement par un test applicatif. | Q2, contrat (`409 DEJA_PRESENT`) |
| **RG5** | Cinq tentatives de code infructueuses consécutives bloquent l'étudiant pendant 2 minutes (`429 TROP_DE_TENTATIVES`) ; une réussite remet le compteur à zéro. | Q4 |
| **RG6** | L'autoévaluation est interdite : un étudiant ne peut jamais relire son propre exercice, ni par tirage, ni par assignation manuelle (`403 AUTO_EVALUATION_INTERDITE`). | Q5 |
| **RG7** | Un exercice a exactement un relecteur : contrainte d'unicité sur l'exercice dans la table des relectures. | Q6 |
| **RG8** | Le relecteur est tiré au sort par le système parmi les étudiants ayant une présence à la session de l'exercice, **quelle que soit la source** de cette présence. | Q7 + décision section 7 |
| **RG9** | Le tirage a lieu immédiatement au dépôt de l'exercice, dans la même transaction. | Q7 + décision section 7 |
| **RG10** | Si aucun étudiant éligible n'existe, l'exercice reste sans relecteur au statut `SANS_RELECTEUR` : il reste visible comme relecture en attente et le formateur peut assigner un relecteur à la main. | Q11 + décision section 7 |
| **RG11** | La note est un entier de 0 à 20 inclus ; toute autre valeur est refusée (`400 NOTE_INVALIDE`). | Q9 |
| **RG12** | La note est **définitive dès son envoi** : aucun second envoi n'est accepté, par personne (`409 RELECTURE_DEJA_RENDUE`). | Q15 (Q10 écartée — section 7) |
| **RG13** | Un commentaire est associé à la note ; il peut être vide, dans la limite de 2 000 caractères. | Q8 + hypothèse section 7 |
| **RG14** | Une relecture non rendue laisse l'exercice « en attente » : cet état doit être visible dans le tableau du formateur, sans ambiguïté avec un exercice relu. | Q11 |
| **RG15** | Un exercice peut être déposé jusqu'à la clôture de la session, y compris après l'expiration du code de présence. | Q12 |
| **RG16** | Le lien d'un exercice est remplaçable tant qu'aucun relecteur n'a consulté le lien ; dès la première consultation, il est figé (`409 RELECTURE_COMMENCEE`). | Q13 |
| **RG17** | Une présence ajoutée par le formateur porte `source = FORMATEUR` et cette origine doit rester visible dans le tableau ; une présence saisie par l'étudiant porte `source = ETUDIANT`. | Q14 |
| **RG18** | Le tableau affiche, par étudiant : ses présences, son nombre d'exercices déposés, la moyenne de ses notes reçues et ses relectures en attente. La moyenne est arrondie à une décimale ; elle est nulle si l'étudiant n'a reçu aucune note. | Q16 + hypothèse section 7 |
| **RG19** | Un étudiant ne peut agir que sur une session de sa propre promotion : un dépôt ou une présence sur la session d'une autre promotion est refusé. | cohérence `promotionId` |
| **RG20** | L'anonymat du relecteur est garanti côté serveur : aucune réponse d'API destinée à l'étudiant relu ne contient l'identité du relecteur. | Q8 |
| **RG21** | Aucune opération n'est possible sur une session clôturée en dehors du rendu des relectures déjà assignées (`409 SESSION_CLOTUREE`). | Q10, Q12, décision section 7 |
| **RG22** | Un lien d'exercice valide est une URL `http` ou `https` absolue, de 500 caractères au plus. | contrat (`400 LIEN_INVALIDE`) |

---

## 7. Zones d'ombre, hypothèses et contradictions tranchées

Cette section est le registre des décisions : chaque point non tranché par le client est
arbitré ici, avec sa justification. Les décisions marquées **[arbitrage client]** ont été
choisies explicitement par le formateur (porteur du besoin) pendant l'étape 1.

### 7.1 Contradiction Q10 / Q15 — la note envoyée est-elle modifiable ?

- **Q10** : « Oui, tant que le formateur n'a pas clôturé la session. »
- **Q15** : « Oui. Une fois que le relecteur a validé, c'est fini, il ne peut plus y revenir. »

Les deux réponses ne peuvent pas être vraies simultanément pour le même événement (« envoi » =
« validation » selon Q15). Trois lectures étaient possibles :

1. Q15 prime — la note est verrouillée dès l'envoi, Q10 est caduque.
2. Q10 prime — la note reste modifiable tant que la session est ouverte, le `409` n'arrivant
   qu'après clôture.
3. Lecture hybride — la note est définitive pour le relecteur, mais le formateur peut rouvrir
   une relecture depuis son tableau.

**Décision retenue : lecture 1.** **[arbitrage client]**

**Justification.** Trois arguments convergent. D'abord le contrat imposé : l'ANNEXE B impose pour
`POST /api/relectures/{id}` l'erreur `409 relecture déjà rendue`, ce qui décrit précisément une
seconde tentative d'envoi refusée, donc une note non modifiable. Ensuite la cohérence
fonctionnelle : la lecture 2 obligerait à distinguer « déjà rendue mais encore ouverte » de
« déjà rendue et clôturée », ce que ni le modèle ni le contrat ne prévoient, et la lecture 3
ajoute un pouvoir de réécriture des notes qui n'apparaît nulle part dans le besoin. Enfin
l'intention exprimée par le client lui-même : Q15 se termine par « C'est plus honnête pour tout
le monde », formule qui exprime une préférence, là où Q10 est une réponse réflexe à une question
fermée. Q10 est donc déclarée périmée et le motif est conservé ici pour la soutenance.

**Conséquence** : un seul chemin d'écriture de la note (`POST /api/relectures/{id}`), un état
terminal `RENDUE`, aucun endpoint de correction, et tout second envoi — du relecteur, de
l'auteur ou du formateur — reçoit `409 { "code": "RELECTURE_DEJA_RENDUE" }` (RG12).

### 7.2 Trou Q7 — qui est « étudiant présent » éligible au tirage ?

Le client écrit « parmi les étudiants présents à cette session » sans dire si l'ajout manuel du
formateur (Q14) compte. Trois lectures :

1. Toute présence à la session, quelle que soit la source.
2. Seulement les présences saisies par l'étudiant lui-même (`source = ETUDIANT`).
3. Présents à la session, avec repli sur les présents d'une autre session de la promotion si le
   vivier est vide.

**Décision retenue : lecture 1 — toute présence, quelle que soit la source.** **[arbitrage client]**

**Justification.** L'ajout manuel existe précisément pour les étudiants en difficulté technique
(Q14 : « ça arrive qu'un étudiant ait un souci de téléphone »). Exclure ces étudiants du tirage
les sanctionnerait une seconde fois pour un incident matériel, ce qui contredit la finalité même
de Q14. La lecture 1 est en outre la seule qui respecte littéralement Q7 (« les étudiants
présents à cette session ») sans introduire de critère caché. La lecture 3 a été écartée parce
que le repli inter-sessions n'est pas demandé et brouille la notion de « cette session ».

**Conséquence** : `source` est une **information d'origine** affichée et conservée, mais elle ne
filtre pas l'éligibilité (RG8).

### 7.3 Trou Q7 — à quel moment le tirage a-t-il lieu ?

Trois lectures : (1) au dépôt, (2) à la clôture de la session, (3) au dépôt puis re-tirage à la
clôture pour les relecteurs défaillants.

**Décision retenue : lecture 1 — tirage immédiat au dépôt.** **[arbitrage client]**

**Justification.** Le dépôt répond alors par un statut directement exploitable (`EN_ATTENTE` ou
`SANS_RELECTEUR`), ce qui rend Q11 observable dès la fin de la session sans attendre une action
du formateur : un exercice « en attente » au tableau est un relecteur assigné qui n'a rien rendu.
La lecture 2 retarderait toute affectation à un acte administratif unique et laisserait le
tableau muet pendant des jours ; la lecture 3 ajoute un second tirage, donc un historique
d'affectation et un risque de changement de relecteur après consultation, incompatible avec
RG16. Le mécanisme de secours de la lecture 3 est remplacé par l'assignation manuelle du
formateur (7.4).

### 7.4 Trou — aucun relecteur éligible, ou seul candidat = l'auteur

Cas visés : personne n'est présent à la session, ou le seul étudiant présent est l'auteur de
l'exercice déposé (donc exclu par Q5). Trois lectures : (1) l'exercice reste en attente et le
formateur débloque manuellement, (2) affectation à la demande au prochain éligible,
(3) le formateur devient relecteur par défaut.

**Décision retenue : lecture 1 — statut `SANS_RELECTEUR`, visible au tableau, déblocage par le
formateur.** **[arbitrage client]**

**Justification.** RG6 interdit absolument l'autoévaluation : le tirage doit donc abandonner
plutôt que désigner l'auteur. La lecture 3 violerait la relecture entre pairs (Q7) et rendrait
l'anonymat de Q8 fictif, puisque l'étudiant saurait qu'un seul acteur restait. La lecture 2
automatise le déblocage mais laisse l'exercice bloqué si personne ne se manifeste, tout en
exigeant une file d'attente et une notion de « prochain éligible » que le besoin ne définit
nulle part. La lecture 1 est la seule qui (a) ne bloque rien en silence, (b) reste entièrement
explicable au formateur, et (c) se vérifie d'un coup d'œil au tableau, conformément à Q11.

**Conséquence** : une opération supplémentaire, `POST /api/exercices/{id}/relecteur`, permet au
formateur de désigner un relecteur ; elle refuse l'auteur avec
`403 AUTO_EVALUATION_INTERDITE` (RG6) et n'est possible que tant que la relecture n'est pas rendue.

### 7.5 Trou Q3 / Q12 — quand une session « finit »-elle ?

Q3 refuse la présence « après la fin de la session » ; Q12 autorise le dépôt « jusqu'à ce que je
clôture la session ». Ces deux formulations impliquent **deux jalons distincts**, ce qui est
retenu comme hypothèse :

- **Fin de validité du code** : `expirationAt`, 15 minutes après l'ouverture (Q2). À partir de
  là, plus aucune présence n'est possible (Q3) — mais les dépôts restent ouverts.
- **Clôture** : action explicite du formateur. Elle ferme les dépôts (Q12), figent les notes
  déjà envoyées (cohérent avec Q10 devenu caduc) et empêche toute nouvelle présence (RG21).

**Justification** : c'est la seule lecture qui satisfait simultanément Q2, Q3, Q10 et Q12.
L'hypothèse « la session finit à l'expiration du code » est écartée car elle rendrait Q12
impossible ; l'hypothèse « tout reste ouvert jusqu'à la clôture » est écartée car elle
contredirait Q2 et Q3.

### 7.6 Trous comblés par nécessité d'implémentation

Ces points ne sont traités nulle part par le client, mais sans eux le parcours est impossible.
Aucun n'est un arbitrage métier : ils découlent du contrat imposé.

| # | Trou | Comblement |
|---|---|---|
| T1 | Le relecteur n'a aucun moyen de lire le lien à relire : l'ANNEXE B n'impose qu'un `POST`. | `GET /api/relectures/{id}`, qui renvoie le lien et le statut **sans** nom d'auteur ni de relecteur (Q8, RG20), et horodate la première consultation (RG16). |
| T2 | L'étudiant relu n'a aucun moyen de consulter sa note (Q8). | `GET /api/etudiants/{id}/exercices`, qui expose statut, note et commentaire, sans l'identité du relecteur. |
| T3 | Le relecteur ne sait pas quels exercices lui sont confiés. | `GET /api/etudiants/{id}/relectures`, qui liste ses relectures en attente et rendues. |
| T4 | La clôture de session (Q10, Q11, Q12) n'existe pas dans le contrat. | `POST /api/sessions/{id}/cloture`, qui horodate la clôture. |
| T5 | L'ajout manuel de présence (Q14) n'existe pas dans le contrat. | `POST /api/sessions/{id}/presences`, qui crée la présence avec `source = FORMATEUR`. |
| T6 | La liste de noms de Q1 suppose des étudiants existants ; `promotionId` implique des promotions. | Tables `promotions` et `etudiants` alimentées par une migration Flyway de données de référence, exposées par `GET /api/promotions/{id}/etudiants`, et erreurs `404 PROMOTION_INCONNUE` / `404 ETUDIANT_INCONNU`. |
| T7 | Q4 impose un blocage après 5 échecs, sans code HTTP associé dans le contrat. | `429 TROP_DE_TENTATIVES`, avec le format d'erreur imposé, ajouté au contrat et documenté ici. |
| T8 | Q13 suppose de savoir si « quelqu'un a commencé à relire ». | Colonne `lien_consulte_at` sur la relecture, renseignée par la consultation du lien (T1). |
| T9 | Aucune identité n'est vérifiée, mais Q5 et Q8 protègent des invariants. | Vérifications serveur systématiques : l'appelant doit être le relecteur désigné pour rendre une note, l'auteur pour remplacer son lien, l'auteur pour consulter sa note (RG6, RG20). |
| T10 | Plusieurs opérations imposées ne portent pas l'identité de l'appelant : `POST /api/relectures/{id}` n'a que `note` et `commentaire`, or il faut vérifier que l'appelant est bien le relecteur désigné (Q5, Q8). | En-tête `X-Etudiant-Id` sur les opérations où l'appelant doit être distingué. Ce complément évite de modifier les corps imposés par le client ; il est documenté dans `api/contrat.yaml`. |

### 7.7 Hypothèses restantes (à confirmer si le client les conteste)

- **H1** — Aucun mot de passe, aucune session serveur : l'identité est déclarative (Q1). Voir §2.1 et ENF7.
- **H2** — Code de présence : 6 caractères alphanumériques, jeu restreint excluant `0/O` et `1/I/L` pour la dictée orale (RG1).
- **H3** — Un commentaire vide est autorisé ; la note, elle, est obligatoire (RG13).
- **H4** — La moyenne du tableau est la moyenne arithmétique des notes reçues, arrondie à une décimale, et vaut 0 en l'absence de note (RG18).
- **H5** — Le formateur est identifié par `formateurId` là où ses opérations l'exigent, mais aucune table d'utilisateurs n'est créée : seul son rôle est reconnu.
- **H6** — Un étudiant peut déposer un exercice pour une session à laquelle il n'était pas présent ; l'absence de présence n'est pas un motif de refus de dépôt (le client ne l'a jamais demandé).
- **H7** — Le blocage de Q4 est comptabilisé par étudiant, toutes sessions confondues, et non par couple (étudiant, session) : le client parle de « bloquez-le », pas de « bloquer sa tentative sur cette session ».
- **H8** — La clôture d'une session n'annule pas les relectures déjà rendues, et ne rend pas relisibles les exercices `SANS_RELECTEUR` : le formateur peut encore assigner un relecteur après clôture, la note restant le seul acte de relecture possible après clôture (RG21).
- **H9** — Le code de présence est comparé après normalisation (espaces de bord retirés, casse replacée en majuscules). Il est dicté à l'oral puis recopié à la main : une casse approximative ne doit pas produire une erreur incompréhensible, et cette tolérance ne réduit pas l'espace des codes. La valeur stockée reste en majuscules.

---

## 8. Contraintes techniques

- **Backend** : Java 17, Spring Boot, Maven avec wrapper `mvnw`/`mvnw.cmd` **commité** (ENF1).
  Structure imposée en trois couches : `controller` → `service` → `repository`, plus `dto`,
  `entity` et `exception`.
- **Accès aux données** : Spring Data JPA. Aucune entité JPA ne traverse la couche HTTP ; les
  contrôleurs ne manipulent que des DTO (ENF3).
- **Migrations** : Flyway, scripts SQL dans `backend/src/main/resources/db/migration`
  (`V1__...`, `V2__...`). `ddl-auto=validate` en permanence, jamais `update` hors tests (ENF5).
  Le SGBD cible et le profil de test sont à figer à l'étape 2 (piste : PostgreSQL en
  développement, base en mémoire pour les tests, scripts SQL écrits de façon portable).
- **Validation** : annotations Bean Validation sur les DTO d'entrée, plus les règles de gestion
  qui dépendent de l'état (RG3, RG5, RG12, RG15, RG16) vérifiées dans les services.
- **Erreurs** : un `@RestControllerAdvice` unique, format `{ "code", "message" }`, jamais de
  stack trace ni de détail technique (ENF4). Une exception de validation renvoie `400 CHAMP_MANQUANT`
  ou `400 VALEUR_INVALIDE` selon le cas, toujours dans le même format.
- **Contrat** : `api/contrat.yaml` est la référence. Tout écart entre le code et le contrat est un
  bug ; le contrat est modifié avant le code, jamais après.
- **Frontend** : React, consommation de l'API via `fetch`, aucune logique métier dupliquée côté
  client : l'interface affiche et propose, le serveur décide (ENF10). Le choix de l'outillage
  (Vite, TypeScript, gestion d'état) est arrêté à l'étape dédiée, pas ici.
- **Tests** : tests de service par règles de gestion et tests d'intégration HTTP vérifiant les
  couples (entrée, code HTTP, corps d'erreur) du contrat (ENF9).
- **Dépôt** : `.gitignore` Java + JavaScript avant le premier commit de code, un commit par idée,
  une branche par issue, une PR par branche avec `Closes #N` (ENF11 et section 10).

---

## 9. Livrables

| # | Livrable | Emplacement |
|---|---|---|
| L1 | Ce cahier des charges | `docs/CAHIER_DES_CHARGES.md` |
| L2 | Diagramme de cas d'utilisation | `docs/diagrammes/D1.md` |
| L3 | Modèle de données / diagramme de classes | `docs/diagrammes/D2.md` |
| L4 | Diagramme de séquence « marquer sa présence » (nominal, 410, 409) | `docs/diagrammes/D3.md` |
| L5 | Diagramme d'états du cycle de vie d'un exercice (bonus) | `docs/diagrammes/D4.md` |
| L6 | Contrat d'API OpenAPI, 5 opérations imposées + compléments | `api/contrat.yaml` |
| L7 | Backlog d'issues, une par incrément livrable | issues GitHub + `docs/issues/` |
| L8 | Backend Spring Boot (couches, DTO, Flyway, gestion d'erreurs) | `backend/` |
| L9 | Frontend React (parcours formateur, étudiant, relecteur) | `frontend/` |
| L10 | Journal de décisions et de vérifications | `JOURNAL.md` |
| L11 | Matière brute du client, conservée verbatim | `CLIENT.md` |

---

## 10. Démarche prévue (Definition of Done)

**Organisation.** Une branche par issue (`feat/#N-slug`), une PR par branche, un commit par idée
avec un message explicite se terminant par `Closes #N`. Les messages `update`, `fix` ou `wip`
sont proscrits. Chaque message de commit et chaque commande Git sont proposés avant d'être
exécutés.

**Ordre de travail prévu.** Analyse (cette étape, clôturée par le commit de jalon
`[JALON] analyse`) → socle technique et migrations Flyway → marche métier par marche métier,
chaque marche correspondant à une issue du backlog → frontend raccordé à mesure → revue finale
contre le contrat.

**Definition of Done — une exigence est finie quand :**

1. Le comportement livré correspond à l'`EFx` visé et respecte les `RGx` citées par l'issue.
2. Le critère « quand … alors … » de l'exigence est vérifié par un test automatisé, en succès
   **et** en échec (code HTTP et corps d'erreur exacts du contrat).
3. Aucune requête base ne figure dans un contrôleur, aucune entité JPA n'est sérialisée : seuls
   des DTO sont exposés (ENF3).
4. Toute erreur nouvelle respecte `{ "code", "message" }` et provient du `@RestControllerAdvice`
   unique ; aucune stack trace n'est renvoyée (ENF4).
5. Les tables concernées sont créées ou modifiées par une migration Flyway, jamais par
   `ddl-auto` (ENF5).
6. `./mvnw verify` (backend) et le build du frontend passent ; le contrat `api/contrat.yaml` est
   à jour si une opération a été ajoutée.
7. La PR est relue contre ce cahier des charges et le contrat, et la branche est fusionnée avec
   `Closes #N` dans le message.
8. Trois lignes ont été ajoutées à `JOURNAL.md` : ce qui a été fait, ce qui a bloqué et combien
   de temps, comment cela a été vérifié.
