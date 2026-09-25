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

**Vérification :** `cd backend && ./mvnw verify` → BUILD SUCCESS, 15 tests (4 unitaires +
11 d'intégration) sur H2 avec les migrations Flyway réelles ; les tests vérifient l'écart
**exact** de 15 minutes entre ouverture et expiration, la conformité du code à l'alphabet
restreint, des codes différents entre deux sessions, et l'absence de champ `trace` dans
chaque réponse d'erreur. `cd frontend && npm run build` → `tsc --noEmit` strict puis build
Vite réussis.
