package fr.kfokam48.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acces en lecture et ecriture aux sessions : aucune logique metier ici. */
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Resolution du code dicte par le formateur vers sa session (EF2). Le code est
     * unique en base, donc la reponse ne peut pas etre ambigue (RG1).
     */
    Optional<Session> findByCode(String code);

    /**
     * Un code doit designer une seule session, sinon « saisir le code » serait
     * ambigu (RG1). La contrainte d'unicite en base est la garante ; cette methode
     * sert a eviter la collision avant l'insertion.
     */
    boolean existsByCode(String code);
}
