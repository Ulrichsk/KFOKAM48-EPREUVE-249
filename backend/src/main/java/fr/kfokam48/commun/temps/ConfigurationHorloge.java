package fr.kfokam48.commun.temps;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Horloge de l'application.
 *
 * <p>Le temps est injecte plutot que lu directement via {@code LocalDateTime.now()} :
 * c'est ce qui rend les regles d'expiration (RG2, RG3, RG5) testables de facon
 * deterministe, sans attendre reellement quinze minutes.</p>
 *
 * <p>Toutes les dates du projet sont exprimees en UTC (ENF6).</p>
 */
@Configuration
public class ConfigurationHorloge {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
