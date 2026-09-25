package fr.kfokam48.exercice;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Validation du lien d'exercice (RG22) : URL absolue {@code http} ou {@code https},
 * 500 caracteres au plus.
 *
 * <p>Trois raisons a ces restrictions :</p>
 * <ul>
 *   <li>le contrat doit repondre {@code 400 LIEN_INVALIDE} a un lien vide ou mal forme ;</li>
 *   <li>la colonne {@code lien} est un {@code VARCHAR(500)} : refuser au-dela evite de
 *       laisser la base produire une erreur technique ;</li>
 *   <li>accepter uniquement {@code http}/{@code https} evite qu'un {@code javascript:} ou
 *       un {@code file:} se retrouve cliquable dans l'interface du relecteur.</li>
 * </ul>
 *
 * <p>Classe volontairement sans etat et sans dependance : la regle se teste seule.</p>
 */
@Component
public class ValidateurLien {

    /** Longueur maximale acceptee, alignee sur {@code exercices.lien VARCHAR(500)}. */
    public static final int LONGUEUR_MAX = 500;

    public boolean estValide(String lien) {
        if (lien == null) {
            return false;
        }
        String candidat = lien.trim();
        if (candidat.isEmpty() || candidat.length() > LONGUEUR_MAX) {
            return false;
        }
        try {
            URI uri = new URI(candidat);
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
                return false;
            }
            String schema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return "http".equals(schema) || "https".equals(schema);
        } catch (URISyntaxException lienIllisible) {
            return false;
        }
    }
}
