**Priorité** : Should
**Exigences** : EF11 · **Règles** : RG20 · **Questions client** : Q8
**Contrat** : `GET /api/etudiants/{id}/exercices` · **Trou** : T2 (§7.6)

## Résultat attendu

Un étudiant relu voit la note et le commentaire reçus, sans pouvoir découvrir qui l'a noté.

## Périmètre

- Liste des exercices d'un étudiant, avec statut et, lorsque la relecture est rendue, la
  note et le commentaire.
- Aucune donnée identifiant le relecteur dans la réponse (ni identifiant, ni nom, ni date
  permettant de le déduire).

## Critères d'acceptation

- Quand un étudiant dont l'exercice a été relu consulte ses exercices, alors il voit la note
  et le commentaire associés.
- Quand sa relecture n'a pas encore été rendue, alors il voit uniquement le statut
  (`EN_ATTENTE` ou `SANS_RELECTEUR`) sans note ni commentaire.
- Quand la réponse est produite, alors elle ne contient **aucun** champ relatif au relecteur
  (`relecteurId`, nom, date d'assignation) : la vérification est un test automatisé, pas une
  relecture visuelle (RG20).
- Quand un étudiant consulte les exercices d'un autre étudiant, alors la réponse est
  `403 { "code": "ACCES_REFUSE" }`.
- Quand l'étudiant n'existe pas, alors la réponse est `404 { "code": "ETUDIANT_INCONNU" }`.
