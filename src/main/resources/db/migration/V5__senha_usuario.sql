-- V5: V2 (salas), V3 (professores e materiais) e V4 (auditoria) estão reservadas por outras PRs.
ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100) NOT NULL;
