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
