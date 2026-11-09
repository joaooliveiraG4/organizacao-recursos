CREATE TABLE sala (
    id UUID PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    capacidade INTEGER NOT NULL CHECK (capacidade > 0),
    localizacao VARCHAR(120) NOT NULL
);

CREATE UNIQUE INDEX uq_sala_codigo_case_insensitive
    ON sala (lower(codigo));
