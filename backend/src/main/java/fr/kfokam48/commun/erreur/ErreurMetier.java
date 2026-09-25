package fr.kfokam48.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * Exception metier de base.
 *
 * <p>Toute regle de gestion violee remonte sous cette forme : elle porte le
 * {@link CodeErreur} attendu par le contrat et le statut HTTP associe. Le
 * {@code GestionnaireErreurs} la traduit en {@link ReponseErreur} sans jamais
 * exposer la trace.</p>
 */
public class ErreurMetier extends RuntimeException {

    private final CodeErreur code;
    private final HttpStatus statut;

    public ErreurMetier(CodeErreur code, HttpStatus statut, String message) {
        super(message);
        this.code = code;
        this.statut = statut;
    }

    public CodeErreur getCode() {
        return code;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}
