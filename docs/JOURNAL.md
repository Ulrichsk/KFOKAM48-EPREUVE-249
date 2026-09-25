# JOURNAL.md — journal de bord KFOKAM48

Une entrée par étape, toujours en trois lignes : **fait**, **blocage (durée)**, **vérification**.

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
