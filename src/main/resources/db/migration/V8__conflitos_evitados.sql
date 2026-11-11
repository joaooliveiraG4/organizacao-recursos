-- RF-12: cada reserva recusada por sobreposição vira um registro, para o
-- relatório contar "conflitos evitados". Sem dados pessoais (LGPD): só o
-- recurso disputado e o período pedido.
CREATE TABLE conflito_evitado (
    id UUID PRIMARY KEY,
    tipo_recurso VARCHAR(12) NOT NULL CHECK (tipo_recurso IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    recurso_id UUID NOT NULL,
    periodo_inicio TIMESTAMPTZ NOT NULL,
    periodo_fim TIMESTAMPTZ NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_conflito_periodo CHECK (periodo_fim > periodo_inicio)
);

CREATE INDEX ix_conflito_evitado_periodo ON conflito_evitado (periodo_inicio, periodo_fim);
