package fr.kfokam48.session;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generation du code de presence (RG1).
 *
 * <p>Deux choix sont assumes :</p>
 * <ul>
 *   <li><b>Alphabet restreint</b> : ni {@code 0} et {@code O}, ni {@code 1}, {@code I}
 *       et {@code L}. Le code est dicte a l'oral et recopie a la main (Q1, Q2) : ces
 *       caracteres sont la premiere source d'erreur de saisie.</li>
 *   <li><b>{@link SecureRandom}</b> : Q4 montre que les etudiants peuvent chercher a
 *       deviner les codes entre eux. Un generateur previsible rendrait cette
 *       protection illusoire.</li>
 * </ul>
 *
 * <p>Six caracteres sur un alphabet de 31 donnent 31^6 combinaisons, soit pres de
 * 887 millions : la collision est improbable, et le service verifie malgre tout
 * l'unicite en base avant d'enregistrer.</p>
 */
@Component
public class GenerateurCodeSession {

    /** Alphabet de 31 caracteres, sans 0, O, 1, I et L (confusions de lecture). */
    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /** Longueur fixe du code (RG1). */
    public static final int LONGUEUR = 6;

    private final SecureRandom alea = new SecureRandom();

    public String generer() {
        StringBuilder code = new StringBuilder(LONGUEUR);
        for (int position = 0; position < LONGUEUR; position++) {
            code.append(ALPHABET.charAt(alea.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
