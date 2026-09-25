package fr.kfokam48;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'application KFOKAM48.
 *
 * <p>Presence aux sessions de cours, depot d'exercices et relecture entre pairs.
 * Le schema est porte par Flyway ({@code src/main/resources/db/migration}) et
 * jamais par Hibernate (ENF5).</p>
 */
@SpringBootApplication
public class Kfokam48Application {

    public static void main(String[] args) {
        SpringApplication.run(Kfokam48Application.class, args);
    }
}
