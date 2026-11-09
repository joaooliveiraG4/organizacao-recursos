-- V5: V2 (salas), V3 (professores e materiais) e V4 (auditoria) estão reservadas por outras PRs.
ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100);

-- Usuários anteriores a esta versão não têm senha conhecida: ficam inativos e
-- com um marcador que nunca confere com BCrypt, até um Administrador redefinir.
UPDATE app_user
SET password_hash = '!sem-senha',
    active = FALSE
WHERE password_hash IS NULL;

ALTER TABLE app_user
    ALTER COLUMN password_hash SET NOT NULL;
