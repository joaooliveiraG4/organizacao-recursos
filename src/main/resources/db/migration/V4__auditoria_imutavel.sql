-- V4: V2 (salas) e V3 (senha de usuário) estão reservadas por outras branches.

-- Todo evento tem autor; ações automáticas usam o autor técnico "Sistema"
-- (00000000-0000-0000-0000-000000000000).
ALTER TABLE audit_event
    ALTER COLUMN actor_id SET NOT NULL;

-- Desempata eventos gravados no mesmo instante, preservando a ordem de inserção.
ALTER TABLE audit_event
    ADD COLUMN sequence_number BIGINT GENERATED ALWAYS AS IDENTITY;

-- A auditoria só aceita inserção: o próprio banco recusa alteração e remoção,
-- inclusive por scripts ou acessos fora da aplicação.
CREATE FUNCTION impedir_alteracao_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_event é imutável: % não permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_event_imutavel
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_auditoria();

CREATE TRIGGER trg_audit_event_sem_truncate
    BEFORE TRUNCATE ON audit_event
    FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_auditoria();
