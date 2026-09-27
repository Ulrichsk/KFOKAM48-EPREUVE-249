# Repriorisation du backlog — enveloppe étape 3

> **Protocole étape 3, point 6** : « Repriorise explicitement le backlog restant
> (certaines issues Should/Could peuvent changer de priorité ou devenir obsolètes à
> cause de ce changement) — documente cette repriorisation dans un commit ou un
> fichier dédié. »
>
> Ce fichier est le document dédié. Le commit qui l'introduit porte ce changement
> dans son message.

## État du backlog avant l'enveloppe

Les 14 issues initiales (#1 à #14) sont **toutes livrées et fermées** à l'étape 2
(`[JALON] v0.1` poussé). Le backlog restant se réduisait aux travaux de clôture de
l'étape 4 : checklist de conformité B1-F3/F1-F3, `CHANGELOG.md`, tri final et jalon
`[JALON] v1.0`.

## Ce que l'enveloppe change

| Élément | Avant | Après | Justification |
|---|---|---|---|
| **Bug #15** (lien vide → `CHAMP_MANQUANT`) | inexistant (défaut découvert) | **Must, traité immédiatement** | Le contrat fait foi ; un client qui reçoit une réponse non documentée ne peut plus s'y fier. Corrigé par test rouge puis correctif (PR #17). |
| **#16** (désigner un relecteur depuis le tableau) | inexistant (découvert — l'endpoint existait, l'écran non) | **Must, traité immédiatement** | Demande explicite du client dans l'enveloppe. Le déblocage manuel (RG10) passe d'« appel API à la main » à une opération d'écran de premier plan. |
| **Issue 04** — blocage des tentatives (Should) | Should, livrée | **Reste Should, inchangée** | Le changement de besoin ne la touche pas : le tirage au sort et la désignation ne passent pas par le code de présence. |
| **Issue 06** — remplacer le lien (Should) | Should, livrée | **Reste Should ; son correctif de conformité (#15) était Must** | La fonctionnalité garde sa priorité d'origine ; seul l'écart au contrat qu'elle portait a été traité en Must. |
| **Issue 11** — consulter sa note (Should) | Should, livrée | **Reste Should, inchangée** | Aucun impact du changement de besoin. |
| **Issue 08** — exercices sans relecteur (Must, livrée) | Déblocage manuel « API seule » | **Renforcé sans changer de priorité** | L'écran du tableau (#16) devient le point d'entrée du déblocage : la fonctionnalité livrée reste valide, elle gagne son interface. |
| **Issues 01, 02, 03, 05, 07, 09, 10, 12, 13** (Must, livrées) | Must | **Restent Must, inchangées** | Cœur du parcours, aucune interaction avec le changement de besoin. |
| **Travaux d'étape 4** (checklist, CHANGELOG, tri final, jalon v1.0) | planifiés | **Restent Must avant livraison** | Ils absorberont en plus la documentation du changement de besoin (déjà faite : CDC RG18/RG23, D2/D4, contrat, journal). |

## Ce que le changement de besoin ne rend pas obsolète

**Rien.** Point vérifié explicitement : le tirage au sort automatique au dépôt reste
la voie normale (RG9), la désignation manuelle reste une voie de secours (RG10) —
l'enveloppe ne la transforme pas en règle, elle en rend l'usage praticable depuis
l'écran. Aucune issue livrée ne devient morte, aucun code livré ne devient inutile.

## Issue créée pendant l'étape 3 qui n'était pas dans le backlog initial

| # | Titre | Priorité | Source | État |
|---|---|---|---|---|
| 15 | Le remplacement d'un lien vide répond `CHAMP_MANQUANT` au lieu de `LIEN_INVALIDE` | Must | Enveloppe étape 3 — bug client | Fermée (PR #17, test rouge d'abord) |
| 16 | Désigner un relecteur aux exercices restés sans relecteur depuis le tableau | Must | Enveloppe étape 3 — changement de besoin | Fermée (PR dédiée) |
