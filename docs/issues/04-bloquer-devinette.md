**Priorité** : Should
**Exigences** : EF13 · **Règles** : RG5 · **Questions client** : Q4
**Hypothèses** : H7 (§7.7) · **Contrat** : erreur `429 TROP_DE_TENTATIVES` (ajout documenté)

## Résultat attendu

Un étudiant qui essaie de deviner les codes est ralenti : au bout de cinq erreurs, il est
bloqué deux minutes.

## Périmètre

- Compteur d'échecs par étudiant, incrémenté à chaque tentative infructueuse
  (code inconnu ou expiré).
- Blocage de 2 minutes après 5 échecs consécutifs, matérialisé par `bloque_jusqu_a`.
- Remise à zéro du compteur après une réussite.

## Critères d'acceptation

- Quand un étudiant enchaîne 5 tentatives infructueuses, alors la 6ᵉ tentative dans les
  2 minutes répond `429 { "code": "TROP_DE_TENTATIVES" }`.
- Quand le délai de 2 minutes est écoulé, alors une nouvelle tentative est traitée
  normalement (elle peut réussir ou échouer, mais elle n'est plus bloquée).
- Quand un étudiant bloqué saisit le **bon** code, alors il reste bloqué jusqu'à la fin du
  délai : le blocage ne peut pas être contourné en devinant juste.
- Quand une tentative réussit, alors le compteur d'échecs repasse à 0.

## Choix documenté

Le blocage est comptabilisé par étudiant, toutes sessions confondues (hypothèse H7) : Q4
dit « bloquez-le deux minutes », sans notion de session. Cette hypothèse est déclarée en
§7.7 du cahier des charges et peut être revue sans impact sur le modèle.
