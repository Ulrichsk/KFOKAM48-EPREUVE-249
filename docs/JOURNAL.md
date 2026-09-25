# JOURNAL.md — journal de bord KFOKAM48

Une entrée par étape, toujours en trois lignes : **fait**, **blocage (durée)**, **vérification**.

---

## Étape 2 — issue 12 « Le formateur voit le tableau complet d'une promotion en une page »

**Fait :** `GET /api/tableau?promotionId=` conforme au contrat (200, une ligne par étudiant :
`etudiantId`, `nom`, `prenom`, `presences`, `presencesFormateur`, `exercicesDeposes`, `moyenne`,
`relecturesEnAttente` ; `404 PROMOTION_INCONNUE` ; `400 CHAMP_MANQUANT` sans paramètre). Le
calcul tient en une projection JPQL agrégée par sous-requêtes corrélées : quatre requêtes au
total, indépendantes du nombre d'étudiants (ENF8), partant de la liste des étudiants pour
qu'aucune ligne ne manque, même sans aucune activité (Q16). La moyenne des notes **reçues** est
arrondie à une décimale côté API (RG18, 0 sans note) — le frontend ne la recalcule jamais.
`presencesFormateur` distingue les ajouts manuels (Q14, RG17) ; les relectures en attente (Q11)
comptent les relectures `EN_ATTENTE` confiées à l'étudiant.

**Blocage :** deux itérations (~30 min). La première requête utilisait `is not true` — invalide
en JPQL — pour exclure l'auteur du calcul de la moyenne ; cette garde était de toute façon
redondante (RG6 est garantie à l'écriture), supprimée et justifiée en commentaire. Puis les
tests ont buté sur la base H2 partagée : les étudiants 5 à 10 portent des compteurs de blocage
posés par `BlocageTentativesCodeIT` (réponses `429` inattendues) et des activités résiduelles.
Le scénario a été reconstruit sur les étudiants 9 (auteur relu, comptes exacts), 7 (relecteur,
compteur purgé) et 2 (peu actif), avec l'état construit en `@BeforeAll` par l'API seule.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **116 tests** (43 unitaires
+ 73 d'intégration), dont `TableauControllerIT` (6 tests) : douze lignes pour douze étudiants,
clés JSON exactement celles du contrat, ligne de l'auteur relu (2 dépôts, moyenne 14.0, rien
en attente), ligne du relecteur (1 présence, 0 dépôt, moyenne 0), ligne d'un étudiant peu
actif restée visible avec ses zeros, 404 et 400 conformes.

---

## Étape 2 — issue 10 « Le relecteur rend sa relecture : une note sur 20 et un commentaire, définitifs »

**Fait :** `POST /api/relectures/{id}` conforme au contrat (200 avec le DTO `RelectureRendue`,
six champs exactement, sans identité de relecteur ; `400 NOTE_INVALIDE` si la note est absente,
décimale ou hors 0-20 ; `400 VALEUR_INVALIDE` au-delà de 2000 caractères de commentaire ;
`403 AUTO_EVALUATION_INTERDITE` si l'appelant est l'auteur, `403 ACCES_REFUSE` sinon ;
`404 RELECTURE_INTROUVABLE` ; `409 RELECTURE_DEJA_RENDUE`). Le rendu passe la relecture à
`RENDUE` et l'exercice à `RELU` dans la même transaction (RG12). La note est définitive dès
l'envoi (Q15, §7.1) : aucun chemin d'écriture n'existe vers une relecture rendue, le second
envoi est refusé avant toute modification et la note d'origine reste inchangée. Le rendu reste
possible après clôture de la session (RG21) : le service ne consulte pas `cloture_at`.

**Blocage :** deux choix de conception arbitrés (~15 min). La note décimale (`14.5`) aurait été
capturée par la liaison JSON comme un « corps illisible » (`VALEUR_INVALIDE`) : le champ est
déclaré `Number` et le service tranche, pour honorer le `NOTE_INVALIDE` imposé par le contrat.
Par ailleurs le contrat distingue l'auteur (`AUTO_EVALUATION_INTERDITE`) d'un simple étranger
(`ACCES_REFUSE`) : deux gardes distinctes. Un doublon de déclaration locale corrigé en une
minute avant la première compilation.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **110 tests** (43 unitaires
+ 67 d'intégration), dont `RendreRelectureIT` (12 tests, étudiants 3/10/11) : nominal avec clés
JSON exactes et anonymat, commentaire absent/vide accepté, bornes 0 et 20 incluses, note
absente/décimale/21/-1 en `NOTE_INVALIDE`, commentaire 2001 caractères en `VALEUR_INVALIDE`,
auteur et intrus refusés sans écriture, second envoi en `409` avec note d'origine vérifiée
inchangée en base, 404 et header obligatoire.

---

## Étape 2 — issue 09 « Le relecteur consulte le lien à relire sans savoir qui l'a écrit »

**Fait :** `GET /api/relectures` et `GET /api/relectures/{id}` conformes au contrat (200 avec
les DTO `RelectureResume` et `RelectureDetail` ; `404 ETUDIANT_INCONNU` / `RELECTURE_INTROUVABLE` ;
`403 ACCES_REFUSE` si l'appelant n'est pas le relecteur désigné, y compris l'auteur ;
`400 CHAMP_MANQUANT` sans le header `X-Etudiant-Id`). La première consultation horodate
`lien_consulte_at` et les suivantes ne l'écrasent jamais : l'instant de la première consultation
fait foi et fige le remplacement du lien par l'auteur (Q13, RG16). L'anonymat de l'auteur (Q8,
RG20) est garanti par construction : aucun DTO ne porte d'identité d'auteur ou de relecteur, ce
qu'un test vérifie sur les clés JSON exactes. Note de portée : la liste n'expose que le statut
`EN_ATTENTE`/`RENDUE` du contrat ; l'écran relecteur n'a besoin que de l'élément en attente,
la note consultable par l'auteur viendra avec l'issue 11.

**Blocage :** deux faux départs d'écriture (~5 min), rattrapés avant toute compilation : un
import corrompu dans le contrôleur et un mauvais type dans le DTO, tous deux remplacés par des
fichiers propres avant le premier `verify`. Un helper de test d'abord écrit de tête se trompait
de repository ; corrigé en injectant `SessionRepository`. Aucun point de blocage réel.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **98 tests** (43 unitaires
+ 55 d'intégration), dont `ConsultationRelectureIT` (7 tests, étudiants 2/8/9) : liste anonyme
avec clés JSON exactement `{ id, exerciceId, statut, assigneAt }`, horodatage de la première
consultation puis non-réécriture lors d'une seconde, refus de l'intrus et de l'auteur en `403`,
`404` relecture et étudiant inconnus, header obligatoire en `400`. Le réveil de la classe
`Exercice` par la requête restait dans la transaction : aucune entité JPA n'est sérialisée.

---

## Étape 1 — ANALYSE (aucun code)

**Fait :** analyse du besoin client et de l'ANNEXE A (16 Q/R) ; rédaction de
`docs/CAHIER_DES_CHARGES.md` (10 sections, 15 EF avec critères « quand…alors », 12 ENF,
22 RG) ; arbitrage documenté de la contradiction Q10/Q15 (Q15 prime, note définitive dès
l'envoi) et de trois trous Q7/Q11 (éligibilité au tirage = toute présence à la session,
tirage immédiat au dépôt, statut `SANS_RELECTEUR` + déblocage manuel du formateur) ;
consignation verbatim de la matière client dans `CLIENT.md` ; diagrammes `docs/diagrammes/D1.md`
(cas d'utilisation), `D2.md` (modèle de données, normatif pour Flyway : 7 tables),
`D3.md` (séquence présence : nominal, 410, 409) et `D4.md` (bonus, cycle de vie d'un
exercice) ; backlog de 13 issues avec corps rédigés dans `docs/issues/` et commandes
`gh issue create` fournies ; `api/contrat.yaml` complété (5 opérations imposées
inchangées + 8 opérations de complément, format d'erreur identique partout).

**Blocage :** premier envoi de l'étape sans les annexes (les gabarits `[Colle ici …]` sont
arrivés vides), donc un aller-retour perdu (~5 min) — les 16 Q/R étaient indispensables
pour citer Q10/Q15 sans les déformer. Second blocage technique : deux passes de correction
sur `api/contrat.yaml` (apostrophes non doublées dans des scalaires YAML entre guillemets
simples, puis un scalaire commençant par un backtick), ~3 min, détectées avant toute
livraison par un contrôle automatisé.

**Vérification :** (1) `python3 -c "import yaml; yaml.safe_load(open('api/contrat.yaml'))"`
→ YAML valide, 12 chemins / 13 opérations, aucune référence interne cassée, codes HTTP
utilisés = {200, 201, 400, 403, 404, 409, 410, 429} ; (2) contrôle manuel que les
5 opérations imposées de `CLIENT.md` §3 sont reprises sans aucune modification de chemin,
verbe, corps ou code ; (3) relecture des diagrammes Mermaid rendus sur
<https://mermaid.live> (coller le contenu de chaque fichier) — non exécutable hors ligne
sur cette machine, à confirmer visuellement.

---

## Étape 2 — issue 01 « Socle » (premier incrément de code)

**Fait :** projet Maven avec wrapper `mvnw` commité ; migrations Flyway `V1` (les 7 tables
annoncées par `docs/diagrammes/D2.md`, SQL portable PostgreSQL/H2) et `V2` (données de
référence : 2 promotions, 16 étudiants — l'application n'est jamais vide au démarrage) ;
couches contrôleur / service / repository avec DTO (`EtudiantDto`, `ReponseErreur`),
`GET /api/promotions/{id}/etudiants` (EF15) et `404 PROMOTION_INCONNUE` ; un unique
`@RestControllerAdvice` produisant `{ code, message }` pour toutes les erreurs, y compris
les routes inconnues, les paramètres mal typés et les exceptions imprévues ; `.gitignore`
Java + JS posé avant tout code ; frontend Vite + React + TypeScript avec couche d'appel API
dédiée (`src/api/`) et écran de liste gérant chargement, erreur et cas nominal.

**Blocage :** aucun blocage fonctionnel ; la seule friction a été de choisir des versions
déjà présentes dans le cache Maven local pour garantir un build reproductible (Spring Boot
3.2.0, Flyway 9.22.3, H2 2.2.224), soit environ 2 minutes. À noter : la création du rôle et
de la base PostgreSQL locaux demande `sudo` et n'a pas été exécutée — elle n'est pas
nécessaire aux tests, qui tournent sur H2.

**Vérification :** (1) `cd backend && ./mvnw verify` → BUILD SUCCESS, 5 tests d'intégration
(Flyway jouée sur H2 en mémoire, aucune base locale requise) ; (2) le scénario d'erreur
imprévue journalise la trace **côté serveur uniquement** — la réponse ne contient que
`code` et `message`, ce qu'affirment les assertions sur l'absence de `trace`,
`stackTrace`, `exception` et `cause` ; (3) `cd frontend && npm install && npm run build` →
`tsc --noEmit` strict sans erreur puis build Vite réussi.

---

## Étape 2 — issue 02 « Ouvrir une session et obtenir un code de présence »

**Fait :** `POST /api/sessions` conforme au contrat (201 avec `id`, `code`, `ouvertureAt`,
`expirationAt` ; `400 CHAMP_MANQUANT` ; `404 PROMOTION_INCONNUE`) : entité `Session`
calculant elle-même son expiration, générateur de code `SecureRandom` de 6 caractères sur
un alphabet de 31 sans `0/O/1/I/L`, service avec horloge UTC injectable, DTO d'entrée
validé par annotations et DTO de sortie en `Instant` ; 4 tests unitaires sur RG1 et
6 tests d'intégration ; côté frontend, écran Formateur d'ouverture de session (code affiché
en grand, états chargement et erreur) et nouvelle couche d'appel API `src/api/sessions.ts`.

**Blocage :** aucun blocage technique. Une décision a été tranchée en cours de route (~5 min) :
RG1 disait « unique parmi les sessions non clôturées », mais la contrainte en base
(`uq_sessions_code`) est globale, et un code réutilisé après clôture rendrait la résolution
du code ambiguë pour un étudiant. RG1 a donc été précisée en « unique toutes sessions
confondues » dans le cahier des charges, plutôt que d'affaiblir la contrainte de la base.
À signaler aussi : ~10 min perdues sur une manipulation Git (commit lancé sans `-m`, ce qui a
ouvert l'éditeur, puis `--amend` qui a réécrit le commit du socle). Le commit du socle était
intact dans `feature/socle-projet` et l'index vide : réparé par un `git reset --soft`, sans
aucune perte de fichier. Leçon retenue : passer le message par l'entrée standard
(`git commit -F -`), jamais par l'éditeur, et ne jamais utiliser `--amend` sur un commit propre.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, 15 tests (4 unitaires +
11 d'intégration) sur H2 avec les migrations Flyway réelles ; les tests vérifient l'écart
**exact** de 15 minutes entre ouverture et expiration, la conformité du code à l'alphabet
restreint, des codes différents entre deux sessions, et l'absence de champ `trace` dans
chaque réponse d'erreur. `cd frontend && npm run build` → `tsc --noEmit` strict puis build
Vite réussis.

---

## Étape 2 — issue 03 « Marquer sa présence avec le code de la session »

**Fait :** `POST /api/presences` conforme au contrat (201 avec `id`, `sessionId`, `etudiantId`,
`source` ; `400 CODE_INCONNU` ; `409 DEJA_PRESENT` ; `410 CODE_EXPIRE`), avec l'ordre de
contrôles du diagramme `D3.md` : session close (`409 SESSION_CLOTUREE`) → expiration du code
(`410`) → appartenance à la promotion de la session (`403 ACCES_REFUSE`, RG19) → unicité de
la présence (RG4). L'unicité reste garantie par la contrainte de la table : une violation en
base est traduite en `409 DEJA_PRESENT`. Ajouts : `SourcePresence` (ETUDIANT/FORMATEUR),
résolution du code par `SessionRepository.findByCode`, `Session.cloturer()` (utilisée par
l'issue 13), normalisation du code saisi (H9, documentée au cahier des charges), et
complément du contrat (`403` et `404` pour cette opération). Côté frontend : écran Étudiant
qui réutilise la liste de noms de Q1 puis saisit le code, avec états de chargement, d'erreur
et de succès.

**Blocage :** aucun blocage. ~5 min de réflexion sur la traduction de la violation de
contrainte : en JPA, une violation détectée au flush condamne la transaction en cours, le
`catch` est donc placé autour de `saveAndFlush` et relance une exception métier — le rollback
étant de toute façon le résultat voulu (aucune ligne écrite, réponse `409`).

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **31 tests** (10 unitaires +
21 d'intégration) sur H2 avec les migrations Flyway réelles. Les assertions les plus
parlantes : un code expiré renvoie `410` **même si l'étudiant est déjà présent** (priorité de
l'expiration sur l'unicité), un second vote ne crée **aucune** seconde ligne
(`countBySession_Id`), un code saisi en minuscules avec des espaces est accepté, et un
étudiant de la promotion 2 sur une session de la promotion 1 reçoit `403 ACCES_REFUSE`.
`cd frontend && npm run build` → `tsc --noEmit` strict puis build Vite réussis.

---

## Étape 2 — issue 04 « Bloquer les tentatives de devinette du code »

**Fait :** blocage après cinq erreurs (EF13, RG5, Q4) : objet de valeur pur
`EtatTentativesCode` (seuil de 5, blocage de 2 minutes, compteur remis à zéro au moment du
blocage pour que l'étudiant ait cinq nouvelles tentatives après le délai), entité
`TentativeCode` et son repository, et surtout `CompteurTentativesCode`, dont les trois
opérations s'exécutent en transaction séparée (`REQUIRES_NEW`) : une exception métier annule
la transaction de la tentative, donc sans transaction distincte l'incrément serait perdu et
le blocage ne se déclencherait jamais. `PresenceService` a été réordonné (étudiant, blocage,
code, clôture, expiration, promotion, unicité) et compte désormais les échecs `CODE_INCONNU`
et `CODE_EXPIRE` tout en remettant le compteur à zéro sur une réussite. `D3.md` est passé en
v1.1 (ordre réel des contrôles + scénario `429`), RG5 a été précisée, et l'aide de l'écran
Étudiant mentionne le blocage.

**Blocage :** ~10 min sur un démarrage de contexte Spring refusé — `findByEtudiant_Id`
échouait avec « No property 'etudiant' found for type 'TentativeCode' », parce que cette
entité mappe une colonne simple `etudiantId` et non une association JPA (contrairement à
`Presence`). Le symptôme était trompeur : les 26 tests d'intégration échouaient tous au
chargement du contexte, avec pour seul message « Failed to load ApplicationContext ». La cause
racine n'est apparue qu'en filtrant les `Caused by` de la sortie Maven. Corrigé en
`findByEtudiantId`.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **41 tests** (15 unitaires +
26 d'intégration). Assertions clés : après 5 échecs, la 6ᵉ tentative renvoie `429` et le
compteur en base est relu pour prouver que l'incrément a bien survécu au rollback ; le bon
code ne contourne pas le blocage (`429`) ; après l'échéance du blocage, la tentative est de
nouveau traitée (`400 CODE_INCONNU`) ; une réussite remet le compteur à zéro ; un autre
étudiant n'est pas affecté (H7). `cd frontend && npm run build` → `tsc --noEmit` strict puis
build Vite réussis.

---

## Étape 2 — issue 05 « Déposer le lien de son exercice »

**Fait :** `POST /api/exercices` conforme au contrat (201 avec `{ id, statut }` ; `400
LIEN_INVALIDE` ; `409 EXERCICE_DEJA_DEPOSE`), avec l'entité `Exercice` (statut
`DEPOSE`/`EN_ATTENTE`/`SANS_RELECTEUR`/`RELU`, et `remplacerLien` déjà prévu pour l'issue 06),
un composant pur `ValidateurLien` pour RG22, et un service dont l'ordre de contrôles est
lien → session → étudiant → clôture → promotion → unicité. Le statut renvoyé est
`EN_ATTENTE`, le tirage au sort de l'issue 07 pourra le faire basculer en `SANS_RELECTEUR`.
Côté frontend : écran Étudiant de dépôt (nom choisi dans la liste, numéro de session, lien)
avec états de chargement, d'erreur et de succès, et traduction du statut renvoyé par l'API.

**Blocage :** aucun blocage technique. Un point d'ergonomie à assumer devant le client : le
contrat impose `{ sessionId, etudiantId, lien }`, donc l'étudiant doit connaître le numéro de
session. C'est cohérent puisque l'écran du formateur l'affiche déjà (« Session #N »), mais
cela mérite d'être signalé — le code de présence, lui, ne suffit pas à identifier la session
pour un dépôt.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **71 tests** (37 unitaires,
dont 22 tests paramétrés sur RG22, et 34 d'intégration). Assertions clés : un dépôt est
accepté sur une session dont le code a expiré depuis 20 minutes (Q12, RG15) ; un second
dépôt sur la même session renvoie `409` avec **une seule** ligne en base ; quatre formes de
lien invalide (absent, relatif, schéma détourné, trop long) sont refusées en
`400 LIEN_INVALIDE` ; dépôt refusé sur session close et pour un étudiant d'une autre
promotion. `cd frontend && npm run build` → `tsc --noEmit` strict puis build Vite réussis.

---

## Étape 2 — issue 07 « Trouver automatiquement un relecteur par tirage au sort »

**Fait :** le tirage a lieu **dans la transaction de dépôt** (RG9, décision §7.3) : au lieu
d'exposer un statut `DEPOSE`, `ExerciceService.deposer` crée l'exercice, tire immédiatement un
relecteur, puis écrit le statut définitif — `EN_ATTENTE` avec une relecture
`assigne_par = SYSTEME` si un candidat existe, `SANS_RELECTEUR` sinon (RG10, décision §7.4).
L'éligibilité tient en **une requête** (`PresenceRepository.findEligiblesPourTirage`) :
présence à la session de l'exercice, **quelle que soit la source** (ETUDIANT ou FORMATEUR,
décision §7.2), auteur exclu (Q5, RG6). Ajouts : `StatutRelecture`, `AssignePar`, entité
`Relecture` (association `OneToOne` sur `exercice_id` unique, qui traduit RG7 par la
structure et non par une convention), `RelectureRepository`, `TirageAuSort` (classe pure
`SecureRandom`, testable sans Spring) et `TirageRelecteur` (assemblage métier, sans
transaction propre pour ne pas rompre celle du dépôt). Transitions `confierAUnRelecteur` et
`marquerSansRelecteur` portées par l'entité `Exercice`, conformément à `docs/diagrammes/D4.md`
qui prévoyait déjà `DEPOSE → EN_ATTENTE/SANS_RELECTEUR`.

**Blocage :** ~5 min sur un `BUILD FAILURE` de compilation — `cannot find symbol: class
ManyToOne` dans `Relecture.java` : l'import `jakarta.persistence.ManyToOne` manquait alors
que l'annotation était utilisée sur `relecteur`. Cause : l'association a été écrite après les
autres et l'import n'a pas suivi. Une fois la compilation passée, 2 tests d'intégration de
l'issue 05 ont échoué (`expected EN_ATTENTE but was SANS_RELECTEUR`) : ils portaient sur des
sessions où personne d'autre n'était présent, et vérifiaient le statut de l'issue 05
(`EN_ATTENTE` écrit en dur) — désormais le tirage s'exécute réellement. Plutôt que d'attendre
`SANS_RELECTEUR` sur un test dont le nom annonce un dépôt nominal, ces deux tests installent
un pair présent (étudiant 5) pour rester sur le chemin `EN_ATTENTE` ; l'absence de candidat
est couverte par les tests dédiés de l'issue 07. Le fichier porte une note expliquant ce
choix. ~10 min au total.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **82 tests** (43 unitaires
+ 39 d'intégration). `TirageAuSortTest` (6 tests purs) prouve le tirage hors de tout contexte
Spring : liste vide ou absente → aucun désigné, un candidat unique toujours désigné, aucune
sortie possible de la liste sur 500 tirages, et au moins 3 candidats distincts sur 300 tirages
à 4 candidats. `TirageRelecteurIT` (5 tests, étudiants 3/4/11/12) vérifie de bout en bout, en
passant par la vraie transaction de `POST /api/exercices` : le relecteur est bien dans la
liste des présents et **jamais** l'auteur, une relecture unique par exercice est créée avec
`assigne_par = SYSTEME` et `note`/`rendu_at` nuls, la présence de source `FORMATEUR` ajoutée
par le formateur rend éligible (§7.2), aucun présent → statut `SANS_RELECTEUR` **et aucune**
relecture en base, et l'auteur seul présent ne peut jamais se relire. Le test de variation
sur 20 tirages à 3 candidats distingue un générateur cassé d'une malchance (probabilité
d'échec ≈ 3⁻¹⁹). `cd frontend && npm run build` → `tsc --noEmit` strict puis build Vite
réussis (aucun changement frontend : l'écran de dépôt traduisait déjà `SANS_RELECTEUR`).

---

## Étape 2 — issue 08 « Voir et débloquer les exercices restés sans relecteur »

**Fait :** `POST /api/exercices/{id}/relecteur` conforme au contrat (201 avec
`{ id, exerciceId, statut, assignePar }` ; `400 CHAMP_MANQUANT` ; `403 AUTO_EVALUATION_INTERDITE`
si le relecteur désigné est l'auteur ; `403 ACCES_REFUSE` s'il n'est pas de la promotion ;
`404 EXERCICE_INTROUVABLE` / `ETUDIANT_INCONNU` ; `409 RELECTURE_DEJA_ASSIGNEE` si l'exercice a
déjà un relecteur, `409 RELECTURE_DEJA_RENDUE` si la note est déjà envoyée). La logique est
portée par un nouveau `RelectureService` — le cycle de vie de la relecture, où viendront
naturellement la consultation du lien (issue 09) et le rendu de note (issue 10) — avec deux DTO
(`DemandeAssignationRelecteur`, `RelectureAssignee`) ; le contrôleur reste HTTP seul et délègue à
ce service, bien que le chemin appartienne aux exercices. L'assignation crée la relecture avec
`assignePar = FORMATEUR` et fait passer l'exercice de `SANS_RELECTEUR` à `EN_ATTENTE` dans la
même transaction. Deux règles ont été précisées et consignées : le relecteur désigné à la main
**n'a pas à être présent** à la session (l'endpoint existe parce que le tirage n'a trouvé
personne — exiger une présence le rendrait inutile), et l'**ordre des contrôles** place l'état de
l'exercice (`409`) avant les refus sur la personne désignée (`403`), comme pour la présence où
`409 SESSION_CLOTUREE` précède `403 ACCES_REFUSE`. `D4.md` est passé en v1.1 (trois lignes de
plus au tableau des transitions refusées + la règle d'ordre) et le §7.4 du cahier des charges a
été complété.

**Blocage :** un arbitrage de périmètre d'abord (~10 min). « Voir et débloquer les exercices
sans relecteur » pouvait se lire comme une demande d'endpoint de liste supplémentaire ; or
`api/contrat.yaml` fait foi, compte 12 chemins, et EF8 est mappé dans `D1.md` à la seule opération
d'assignation, la visibilité passant par `GET /api/tableau` (issue 12). Décision : **contrat
inchangé** — étendre le contrat gelé pour un besoin que le tableau couvre déjà aurait été une
dérive. Second point, technique : le cas `409 RELECTURE_DEJA_RENDUE` n'est **pas atteignable par
l'API** aujourd'hui, puisque rien ne rend encore de note (c'est l'issue 10). Deux options — ne pas
implémenter cette branche avant l'issue 10, ou l'implémenter et la vérifier en amenant la ligne à
l'état `RENDUE` directement en base. La seconde a été retenue : un refus annoncé par le contrat
doit exister dès qu'il est observable, et le test l'explique en commentaire en précisant qu'il
respecte les contraintes `CHECK` du schéma (statut autorisé, note entre 0 et 20). ~5 min.

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, **91 tests** (43 unitaires +
48 d'intégration), dont `AssignationRelecteurIT` (9 tests) qui exerce l'endpoint de bout en bout,
chaque cas partant d'un exercice **réellement** resté `SANS_RELECTEUR` (session ouverte par
l'API, dépôt sans aucune présence). Assertions clés : la réponse contient exactement `id`,
`exerciceId`, `statut` et `assignePar` (aucun champ de plus, aucun `trace`), la relecture est en
base avec `note`/`renduAt`/`lienConsulteAt` nuls, l'exercice est bien repassé `EN_ATTENTE` avec
**une seule** relecture ; l'assignation aboutit **sans aucune présence** à la session (la raison
d'être de l'endpoint) ; l'auteur proposé renvoie `403 AUTO_EVALUATION_INTERDITE` **et** laisse
l'exercice `SANS_RELECTEUR` sans relecture ; un étudiant de la promotion 2 renvoie `403
ACCES_REFUSE` ; un second relecteur renvoie `409 RELECTURE_DEJA_ASSIGNEE` en **conservant** le
relecteur d'origine ; un exercice déjà relu renvoie `409 RELECTURE_DEJA_RENDUE` ; corps vide,
exercice inconnu et étudiant inconnu donnent `400 CHAMP_MANQUANT`, `404 EXERCICE_INTROUVABLE` et
`404 ETUDIANT_INCONNU`. `cd frontend && npm run build` → `tsc --noEmit` strict puis build Vite
réussis, sans aucun changement frontend : l'écran de déblocage dépend de la liste du tableau de
bord, qui appartient à l'issue 12.
