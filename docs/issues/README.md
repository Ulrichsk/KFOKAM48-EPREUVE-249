# Backlog — issues prêtes à créer

Chaque fichier de ce dossier contient le **corps** d'une issue GitHub (priorité, exigences,
critères d'acceptation « quand … alors … », règles de gestion et questions client citées).
Les titres sont orientés résultat utilisateur, pas tâche technique.

Tous les renvois pointent vers `docs/CAHIER_DES_CHARGES.md` (EFx, RGx, §x) et
`api/contrat.yaml` pour les chemins et codes HTTP.

## Index

| # | Fichier | Titre de l'issue | Priorité | EF | RG | Q |
|---|---|---|---|---|---|---|
| 1 | `01-socle-projet.md` | Socle : le projet démarre, la base est versionnée et toute erreur sort au format imposé | Must | ENF1-5, ENF11, EF15 | RG1, RG19 | Q1 |
| 2 | `02-ouvrir-session.md` | Ouvrir une session et obtenir un code de présence | Must | EF1 | RG1, RG2 | Q2 |
| 3 | `03-marquer-presence.md` | Marquer sa présence avec le code de la session | Must | EF2, EF3 | RG2, RG3, RG4, RG19 | Q2, Q3 |
| 4 | `04-bloquer-devinette.md` | Bloquer les tentatives de devinette du code | Should | EF13 | RG5 | Q4 |
| 5 | `05-deposer-exercice.md` | Déposer le lien de son exercice | Must | EF5 | RG15, RG19, RG22 | Q12 |
| 6 | `06-remplacer-lien.md` | Remplacer le lien tant que la relecture n'a pas commencé | Should | EF6 | RG16, RG20, RG21 | Q13 |
| 7 | `07-tirage-relecteur.md` | Trouver automatiquement un relecteur par tirage au sort | Must | EF7 | RG6, RG7, RG8, RG9 | Q5, Q6, Q7 |
| 8 | `08-sans-relecteur.md` | Voir et débloquer les exercices restés sans relecteur | Must | EF8 | RG6, RG10, RG14 | Q5, Q11 |
| 9 | `09-consulter-lien-a-relire.md` | Consulter le lien de l'exercice à relire sans savoir qui l'a écrit | Must | EF9 | RG16, RG20 | Q8, Q13 |
| 10 | `10-rendre-relecture.md` | Rendre sa relecture : une note sur 20 et un commentaire | Must | EF10 | RG11, RG12, RG13, RG21 | Q9, Q15 |
| 11 | `11-consulter-sa-note.md` | Consulter la note reçue sans connaître le relecteur | Should | EF11 | RG20 | Q8 |
| 12 | `12-tableau-bord.md` | Afficher le tableau de bord d'une promotion en une page | Must | EF12 | RG17, RG18 | Q11, Q14, Q16 |
| 13 | `13-cloturer-session.md` | Clôturer la session quand le cours est terminé | Must | EF14 | RG21 | Q10, Q11, Q12 |

## Ordre de réalisation conseillé

`01` (socle) → `02` → `03` → `05` → `07` (cœur du parcours) → `08` → `09` → `10` →
`12` → `13`, puis les trois issues *Should* : `04`, `06`, `11`.

Les issues `06` et `09` sont liées : le remplacement du lien n'est vérifiable que si la
consultation du lien est tracée (`lien_consulte_at`).

## Convention Git associée

Une branche par issue (`feat/#N-slug`), une PR par branche, un message de commit qui ferme
l'issue (`Closes #N`). Le message de chaque commit est proposé avant exécution.
