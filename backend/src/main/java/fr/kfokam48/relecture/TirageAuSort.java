package fr.kfokam48.relecture;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Tirage au sort d'un element dans une liste (Q7).
 *
 * <p>Classe sans etat metier et sans dependance : elle ne connait ni les etudiants, ni
 * la base, ce qui la rend testable seule. Le choix du {@link SecureRandom} est
 * deliberé : le formateur ne doit pas pouvoir anticiper le relecteur, et ce choix est
 * deja celui utilise pour les codes de presence (RG1).</p>
 *
 * <p>Une liste vide ne produit pas d'erreur mais un resultat vide : c'est au service de
 * decider ce qu'implique l'absence de candidat (ici, un exercice sans relecteur, RG10).</p>
 */
@Component
public class TirageAuSort {

    private final SecureRandom alea = new SecureRandom();

    public <T> Optional<T> choisirParmi(List<T> candidats) {
        if (candidats == null || candidats.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidats.get(alea.nextInt(candidats.size())));
    }
}
