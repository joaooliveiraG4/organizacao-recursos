# API de relatórios (RF-12)

`GET /api/v1/relatorios/utilizacao?inicio=2026-11-01T00:00:00Z&fim=2026-12-01T00:00:00Z`

Somente Responsável ou Administrador (Solicitante recebe `403`). `inicio` e `fim` são instantes ISO 8601; `fim` precisa ser posterior a `inicio` (senão `400`).

Entra toda reserva cujo período cruza `[inicio, fim)`. Os números são calculados direto do banco, no momento da consulta.

```json
{
  "inicio": "2026-11-01T00:00:00Z",
  "fim": "2026-12-01T00:00:00Z",
  "reservasPorEstado": {"SOLICITADA": 2, "APROVADA": 0, "EM_USO": 0, "CONCLUIDA": 0,
                        "REJEITADA": 0, "CANCELADA": 1, "NAO_COMPARECEU": 0},
  "recursos": [
    {"tipo": "PROFESSOR", "recursoId": "…", "codigo": "PROF-1", "reservas": 1, "horas": 2.0},
    {"tipo": "SALA", "recursoId": "…", "codigo": "B-101", "reservas": 2, "horas": 5.0}
  ],
  "conflitosEvitados": 1
}
```

- **`reservasPorEstado`**: todas as reservas do período, inclusive canceladas e rejeitadas.
- **`recursos`**: utilização por sala, professor e material. Contam só reservas `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`. `horas` é a duração inteira de cada reserva somada; para professores, é a **carga horária** no período.
- **`conflitosEvitados`**: tentativas de reserva recusadas com `409` por sobreposição cujo período pedido cruza o do relatório. Cada recusa é gravada numa transação própria (V8, tabela `conflito_evitado`), porque a transação da reserva é desfeita.
- Sem dados pessoais: o relatório não expõe quem pediu cada reserva.

Limitações: as horas não são recortadas nas bordas do período (uma reserva que começa antes de `inicio` conta inteira); quantidade de material não é considerada (ADR 0005).
