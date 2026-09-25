package fr.kfokam48.relecture;

/**
 * Corps de {@code POST /api/relectures/{id}} (schema {@code DemandeRelecture} du
 * contrat) : {@code { note, commentaire }}.
 *
 * <p>La note est declaree en {@link Number} et non en {@code Integer} : le contrat
 * impose {@code 400 NOTE_INVALIDE} pour une note <b>decimale</b> (ex. {@code 14.5}),
 * alors qu'une liaison JSON en {@code Integer} produirait un simple « corps illisible »
 * avant tout traitement metier. C'est le service qui tranche : entier de 0 a 20 (Q9),
 * sinon {@code NOTE_INVALIDE} ; elle est definitive des son envoi (Q15, decision 7.1).</p>
 *
 * <p>Aucune annotation de validation ici : le contrat distingue {@code NOTE_INVALIDE}
 * (note absente, decimale ou hors bornes) de {@code CHAMP_MANQUANT} (autres champs), et
 * c'est le service qui porte cette regle de presentation.</p>
 */
public record DemandeRelecture(

        Number note,

        String commentaire
) {
}
