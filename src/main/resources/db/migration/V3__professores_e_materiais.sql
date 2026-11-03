CREATE TABLE professor (
    id UUID PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    competencia VARCHAR(120) NOT NULL
);

CREATE UNIQUE INDEX uq_professor_codigo_case_insensitive
    ON professor (lower(codigo));

CREATE TABLE material (
    id UUID PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0)
);

CREATE UNIQUE INDEX uq_material_codigo_case_insensitive
    ON material (lower(codigo));
