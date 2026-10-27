-- V3 (não V2) porque a branch de cadastro de salas já reserva V2__salas.sql.
ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100) NOT NULL;
