-- RF-08: recurso em manutenção não pode ser reservado. A coluna existe nas três
-- tabelas para a checagem da reserva ser uniforme; a API só marca sala e material.
ALTER TABLE sala ADD COLUMN em_manutencao BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE professor ADD COLUMN em_manutencao BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE material ADD COLUMN em_manutencao BOOLEAN NOT NULL DEFAULT FALSE;
