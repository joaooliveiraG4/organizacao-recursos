CREATE TABLE reserva (
    id UUID PRIMARY KEY,
    solicitante_id UUID NOT NULL REFERENCES app_user (id),
    inicio TIMESTAMPTZ NOT NULL,
    fim TIMESTAMPTZ NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'SOLICITADA',
    CONSTRAINT ck_reserva_periodo CHECK (fim > inicio),
    CONSTRAINT ck_reserva_estado CHECK (
        estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA',
                   'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')
    )
);

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reserva_recurso (
    reserva_id UUID NOT NULL REFERENCES reserva (id),
    tipo_recurso VARCHAR(12) NOT NULL CHECK (tipo_recurso IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    recurso_id UUID NOT NULL,
    periodo TSTZRANGE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (reserva_id, tipo_recurso, recurso_id),
    CONSTRAINT ck_reserva_recurso_periodo CHECK (
        NOT isempty(periodo) AND NOT lower_inf(periodo) AND NOT upper_inf(periodo)
        AND lower_inc(periodo) AND NOT upper_inc(periodo)
    ),
    CONSTRAINT ex_reserva_recurso_sobreposicao EXCLUDE USING gist (
        tipo_recurso WITH =, recurso_id WITH =, periodo WITH &&
    ) WHERE (ativo)
);

CREATE UNIQUE INDEX uq_reserva_sala_professor
    ON reserva_recurso (reserva_id, tipo_recurso)
    WHERE tipo_recurso IN ('SALA', 'PROFESSOR');
