package fr.kfokam48.relecture;

/**
 * Etat d'une relecture (voir {@code docs/diagrammes/D4.md}).
 *
 * <p>{@code EN_ATTENTE} signifie « assignee, pas encore rendue » : c'est ce que le
 * formateur doit voir clairement dans son tableau (Q11, RG14). {@code RENDUE} est un
 * etat terminal, la note etant definitive des son envoi (Q15, RG12).</p>
 */
public enum StatutRelecture {

    EN_ATTENTE,

    RENDUE
}
