package fr.kfokam48.session;

import fr.kfokam48.referentiel.Promotion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire des regles de duree de vie d'une session (RG2, RG3, RG21).
 *
 * <p>Test unitaire pur : aucune base, aucun contexte Spring, aucune attente reelle.
 * C'est precisement ce que permet l'horloge injectee cote service : la regle
 * d'expiration se verifie a l'instant precis qui nous interesse, et non « 15
 * minutes plus tard ».</p>
 */
class SessionTest {

    private static final LocalDateTime OUVERTURE = LocalDateTime.of(2026, 9, 25, 8, 0);

    private final Promotion promotion =
            new Promotion("L3 Informatique", LocalDateTime.of(2026, 1, 1, 0, 0));

    private Session nouvelleSession() {
        return new Session(promotion, "Algorithmique — seance 4", "K7M2QP", OUVERTURE);
    }

    @Test
    @DisplayName("l'expiration vaut exactement 15 minutes apres l'ouverture (RG2)")
    void l_expiration_vaut_quinze_minutes_apres_l_ouverture() {
        Session session = nouvelleSession();

        assertThat(session.getOuvertureAt()).isEqualTo(OUVERTURE);
        assertThat(session.getExpirationAt()).isEqualTo(OUVERTURE.plusMinutes(15));
        assertThat(Session.DUREE_VALIDITE_CODE).isEqualTo(java.time.Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("le code est encore valide avant l'echeance (RG3)")
    void le_code_reste_valide_avant_l_echeance() {
        Session session = nouvelleSession();

        assertThat(session.codeExpireA(OUVERTURE)).isFalse();
        assertThat(session.codeExpireA(OUVERTURE.plusMinutes(14))).isFalse();
        assertThat(session.codeExpireA(OUVERTURE.plusSeconds(899))).isFalse();
    }

    @Test
    @DisplayName("le code est encore valide a l'instant meme de l'echeance (RG3)")
    void le_code_reste_valide_a_l_instant_de_l_echeance() {
        // Q2 dit que le code « ne marche plus » apres, donc pas pendant.
        assertThat(nouvelleSession().codeExpireA(OUVERTURE.plusMinutes(15))).isFalse();
    }

    @Test
    @DisplayName("le code est expire une seconde apres l'echeance (RG3)")
    void le_code_est_expire_une_seconde_apres_l_echeance() {
        Session session = nouvelleSession();

        assertThat(session.codeExpireA(OUVERTURE.plusMinutes(15).plusSeconds(1))).isTrue();
        assertThat(session.codeExpireA(OUVERTURE.plusHours(3))).isTrue();
    }

    @Test
    @DisplayName("une session neuve n'est pas close (RG21)")
    void une_session_neuve_n_est_pas_close() {
        Session session = nouvelleSession();

        assertThat(session.getClotureAt()).isNull();
        assertThat(session.estCloturee()).isFalse();
    }

    @Test
    @DisplayName("la cloture est idempotente : la premiere date est conservee (RG21)")
    void la_cloture_est_idempotente() {
        Session session = nouvelleSession();
        LocalDateTime premiereCloture = OUVERTURE.plusHours(2);

        session.cloturer(premiereCloture);
        session.cloturer(OUVERTURE.plusHours(5));

        assertThat(session.estCloturee()).isTrue();
        assertThat(session.getClotureAt()).isEqualTo(premiereCloture);
    }
}
