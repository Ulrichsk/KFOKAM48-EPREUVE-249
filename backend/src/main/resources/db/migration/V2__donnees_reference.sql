-- =====================================================================
-- V2 — Donnees de reference (T6 du cahier des charges)
-- Q1 : « l'etudiant choisit son nom dans une liste ». Sans etudiants en base,
-- la liste serait vide et l'application inutilisable : ces donnees rendent
-- l'application exploitable des le premier demarrage (jamais vide).
-- Identifiants fixes puis redemarrage des sequences, pour que les insertions
-- de l'application ne collisionnent pas avec les donnees de reference.
-- =====================================================================

INSERT INTO promotions (id, nom, created_at) VALUES
    (1, 'L3 Informatique', CURRENT_TIMESTAMP),
    (2, 'M1 Genie Logiciel', CURRENT_TIMESTAMP);

INSERT INTO etudiants (id, promotion_id, nom, prenom, created_at) VALUES
    (1,  1, 'Diallo',  'Amina',    CURRENT_TIMESTAMP),
    (2,  1, 'Fotso',   'Bertrand', CURRENT_TIMESTAMP),
    (3,  1, 'Kamga',   'Chantal',  CURRENT_TIMESTAMP),
    (4,  1, 'Mbarga',  'Didier',   CURRENT_TIMESTAMP),
    (5,  1, 'Ngo',     'Estelle',  CURRENT_TIMESTAMP),
    (6,  1, 'Njoya',   'Franck',   CURRENT_TIMESTAMP),
    (7,  1, 'Onana',   'Ginette',  CURRENT_TIMESTAMP),
    (8,  1, 'Owona',   'Herve',    CURRENT_TIMESTAMP),
    (9,  1, 'Sadou',   'Ines',     CURRENT_TIMESTAMP),
    (10, 1, 'Tchoumi', 'Jules',    CURRENT_TIMESTAMP),
    (11, 1, 'Yemga',   'Karine',   CURRENT_TIMESTAMP),
    (12, 1, 'Zogo',    'Leon',     CURRENT_TIMESTAMP),
    (13, 2, 'Abena',   'Marc',     CURRENT_TIMESTAMP),
    (14, 2, 'Bessala', 'Nadege',   CURRENT_TIMESTAMP),
    (15, 2, 'Ebode',   'Olivier',  CURRENT_TIMESTAMP),
    (16, 2, 'Mvondo',  'Paule',    CURRENT_TIMESTAMP);

ALTER TABLE promotions ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE etudiants  ALTER COLUMN id RESTART WITH 1000;
