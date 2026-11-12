# Sequência crítica — criar reserva (RF-04, RF-05, RF-06, RF-08, RF-11)

É o fluxo com mais risco do sistema: duas pessoas podem pedir o mesmo recurso no mesmo horário. O banco é a última barreira.

```mermaid
sequenceDiagram
    autonumber
    actor S as Solicitante
    participant C as ReservaController
    participant R as ReservaService
    participant DB as PostgreSQL
    participant A as AuditoriaService
    participant N as Notificador (após commit)

    S->>C: POST /api/v1/reservas (HTTP Basic)
    C->>R: criar(solicitante, período, sala, professor, materiais)
    R->>R: valida período (fim depois do início) → senão 400
    R->>DB: SELECT restrito ... FOR NO KEY UPDATE (cada recurso, em ordem)
    DB-->>R: recurso travado (ou 404 se não existe)
    R->>DB: SELECT em_manutencao → se sim, 409 (RF-08)
    R->>DB: INSERT reserva (APROVADA ou SOLICITADA se restrito)
    loop cada recurso
        R->>DB: INSERT reserva_recurso (tstzrange)
        alt horário já ocupado
            DB-->>R: 23P01 (EXCLUDE constraint)
            R->>DB: grava conflito_evitado (transação própria)
            R-->>C: 409 — toda a transação é desfeita
        end
    end
    R->>A: registrar RESERVA_CRIADA (mesma transação, MANDATORY)
    R->>N: publica evento (só sai se o commit acontecer)
    R-->>C: reserva
    C-->>S: 201 Created
    Note over DB,N: COMMIT
    N-)N: envia notificação em segundo plano (timeout 2 s, só 2xx = enviada)
```

## Por que é seguro

- **Travas em ordem fixa** (tipo, depois id): dois pedidos com os mesmos recursos não entram em deadlock.
- **Restrição `EXCLUDE` no banco** (V6): mesmo que dois pedidos passem pela aplicação ao mesmo tempo, só um `INSERT` sobrevive. Provado em `ReservaIntegrationTest` e na [disputa real](../performance/resultado-01.md).
- **Auditoria na mesma transação**: se a auditoria falhar, a reserva não existe.
- **Notificação depois do commit**: um provedor lento ou fora do ar não segura nem desfaz a reserva.
