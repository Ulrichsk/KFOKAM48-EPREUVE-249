package fr.kfokam48.commun.erreur;

/**
 * Codes d'erreur du contrat {@code api/contrat.yaml}.
 *
 * <p>Ils sont stables et destines au client : le champ {@code code} d'une reponse
 * d'erreur ne contient jamais autre chose qu'une de ces valeurs. Le couple
 * (code, statut HTTP) est fige par le contrat.</p>
 */
public enum CodeErreur {

    // ---- 400 : la requete est mal formee ou une donnee est refusee ----------
    CHAMP_MANQUANT,
    VALEUR_INVALIDE,
    CODE_INCONNU,
    LIEN_INVALIDE,
    NOTE_INVALIDE,

    // ---- 403 : l'appelant agit sur une ressource qui n'est pas la sienne ----
    ACCES_REFUSE,
    AUTO_EVALUATION_INTERDITE,

    // ---- 404 : la ressource visee n'existe pas ------------------------------
    PROMOTION_INCONNUE,
    ETUDIANT_INCONNU,
    SESSION_INTROUVABLE,
    EXERCICE_INTROUVABLE,
    RELECTURE_INTROUVABLE,
    RESSOURCE_INTROUVABLE,

    // ---- 405 : mauvais verbe HTTP ------------------------------------------
    METHODE_NON_AUTORISEE,

    // ---- 409 : conflit avec l'etat courant ---------------------------------
    DEJA_PRESENT,
    SESSION_CLOTUREE,
    EXERCICE_DEJA_DEPOSE,
    RELECTURE_DEJA_RENDUE,
    RELECTURE_DEJA_ASSIGNEE,
    RELECTURE_COMMENCEE,

    // ---- 410 : le code de presence a expire (Q2) ---------------------------
    CODE_EXPIRE,

    // ---- 429 : trop de tentatives de code (Q4) -----------------------------
    TROP_DE_TENTATIVES,

    // ---- 500 : erreur inattendue, message toujours generique --------------
    ERREUR_INTERNE
}
