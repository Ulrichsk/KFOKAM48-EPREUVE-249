# CLIENT.md — Matière brute fournie par le client

> Fichier source de l'étape 1 (ANALYSE). Aucune interprétation ici : le texte ci-dessous
> est reproduit tel quel. Les décisions d'interprétation sont dans
> `docs/CAHIER_DES_CHARGES.md`, section 7.

## 1. Le besoin (tel qu'écrit par le client)

1. Un formateur ouvre une session de cours et obtient un code de présence.
2. Un étudiant saisit ce code pour marquer sa présence.
3. Un étudiant dépose le lien de son exercice pour une session.
4. Un étudiant est assigné à la relecture de l'exercice d'un pair : note et commentaire.
5. Le formateur voit un tableau : présence et moyenne des notes par étudiant.

## 2. ANNEXE A — Les 16 questions/réponses déjà posées au client

- **Q1** — Faut-il un mot de passe ? Non, l'étudiant choisit son nom dans une liste. Ne perdez pas de temps là-dessus.
- **Q2** — Le code de présence expire-t-il ? Oui, 15 minutes après l'ouverture de la session. Après, il ne marche plus.
- **Q3** — Peut-on marquer sa présence après la fin de la session ? Non.
- **Q4** — Et s'il se trompe de code plusieurs fois ? Qu'il réessaie. Au bout de cinq erreurs, bloquez-le deux minutes, sinon ils vont deviner les codes entre eux.
- **Q5** — Un étudiant peut-il relire son propre exercice ? Jamais. C'est le principe même.
- **Q6** — Combien de relecteurs par exercice ? Un seul.
- **Q7** — Qui choisit le relecteur ? Le système, au hasard, parmi les étudiants présents à cette session.
- **Q8** — L'étudiant relu voit-il sa note ? Oui, la note et le commentaire. Mais pas le nom du relecteur.
- **Q9** — Sur combien est la note ? Sur 20, en nombres entiers.
- **Q10** — Un relecteur peut-il corriger sa note après l'avoir envoyée ? Oui, tant que le formateur n'a pas clôturé la session.
- **Q11** — Et si le relecteur ne rend jamais sa relecture ? L'exercice reste « en attente » et je dois le voir clairement dans mon tableau.
- **Q12** — Peut-on déposer son exercice après la fin de la session ? Oui, jusqu'à ce que je clôture la session. Certains n'ont pas de connexion le soir même.
- **Q13** — Peut-on remplacer le lien de son exercice ? Oui, tant que personne n'a commencé à le relire.
- **Q14** — Puis-je ajouter une présence à la main ? Oui, ça arrive qu'un étudiant ait un souci de téléphone. Mais il faut que ça se voie : marquez « ajouté par le formateur ».
- **Q15** — La note est-elle définitive une fois envoyée ? Oui. Une fois que le relecteur a validé, c'est fini, il ne peut plus y revenir. C'est plus honnête pour tout le monde.
- **Q16** — Que dois-je voir dans mon tableau ? Par étudiant : sa présence à chaque session, combien d'exercices il a déposés, la moyenne des notes reçues, et les relectures qu'il doit encore faire.

## 3. ANNEXE B — Contrat d'API imposé (5 opérations, à respecter tel quel)

- `POST /api/sessions` `{ titre, promotionId }` → `201 { id, code, ouvertureAt, expirationAt }` · erreur `400` champ manquant
- `POST /api/presences` `{ code, etudiantId }` → `201 { id, sessionId, etudiantId, source }` · erreurs `400` code inconnu / `409` déjà présent / `410` code expiré
- `POST /api/exercices` `{ sessionId, etudiantId, lien }` → `201 { id, statut }` · erreurs `400` lien invalide / `409` exercice déjà déposé
- `POST /api/relectures/{id}` `{ note, commentaire }` → `200` · erreurs `400` note hors 0–20 ou non entière / `403` relecture de son propre exercice / `409` relecture déjà rendue
- `GET /api/tableau?promotionId=` → `200 [ { etudiantId, nom, presences, exercicesDeposes, moyenne, relecturesEnAttente } ]` · erreur `404` promotion inconnue

**Format d'erreur imposé, pour toutes les erreurs sans exception :**

```json
{ "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
```

Le champ `source` d'une présence vaut `ETUDIANT` ou `FORMATEUR` (voir Q14).
