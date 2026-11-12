# Resultado 01 — disputa real pela mesma sala (RF-06)

Status: **executado em 12/11/2026, 17:28 (BRT, UTC-3)**. Cenário aprovado.

## Ambiente

| Item | Valor |
|---|---|
| Aplicação | clone limpo de `main` no commit `c30650735b80ce13f0fa58caac4bf2bb64f579ff`, subido com `mvnw spring-boot:run` seguindo o README |
| Banco | PostgreSQL 17.11 do `compose.yaml` (Docker Desktop), migrations V1–V9 aplicadas pelo Flyway |
| Java | OpenJDK 21.0.7 |
| Topologia | cliente, API e banco na mesma máquina (notebook Windows 11); sem rede entre eles |
| Cliente | 20 processos `curl` disparados em paralelo pelo shell, um pedido cada |
| Autenticação | HTTP Basic com usuário Solicitante sintético `qa-solic@exemplo.test`, senha gerada para a sessão e não versionada |

## Fixture

Sala nova `QA-…` criada pela API pelo Administrador temporário (`qa-admin@exemplo.test`, via `BOOTSTRAP_ADMIN_*`). Antes da disputa a sala não tinha nenhuma alocação, então o horário estava livre.

Pedido idêntico para os 20 clientes:

```json
{"salaId":"<id da sala QA>","inicio":"2026-12-01T13:00:00Z","fim":"2026-12-01T15:00:00Z"}
```

## Resultado

| Medida | Valor |
|---|---|
| Pedidos | 20 |
| Aceitos (`201`) | **1** |
| Conflitos esperados (`409`) | **19** |
| Erros técnicos (5xx, timeout, conexão) | **0** |
| Tempo por pedido: mínimo / mediana / p95 / máximo | 107 ms / 180 ms / 250 ms / 260 ms |
| Alocações ativas da sala no banco | **1** |
| Reservas persistidas do solicitante | **1** |
| Conflitos gravados em `conflito_evitado` (RF-12) | **19** |
| Auditoria `RESERVA_CRIADA` da reserva aceita | **1** |

Critério de aprovação (do [modelo de relatório](../testes/relatorio-carga-modelo.md)): uma reserva no banco, uma resposta de aceite, os demais pedidos como conflito esperado e nenhum erro técnico. **Atendido.**

## Comando (sem credenciais)

```bash
for i in $(seq 1 20); do
  curl -s -o /dev/null -w "%{http_code} %{time_total}\n" -u "$QA_USUARIO" \
    -H 'Content-Type: application/json' -d "$PEDIDO" \
    http://localhost:8080/api/v1/reservas > disputa/$i.txt &
done; wait
```

Conferência no banco:

```sql
SELECT count(*) FROM reserva_recurso WHERE recurso_id = :sala AND ativo;   -- 1
SELECT count(*) FROM conflito_evitado WHERE recurso_id = :sala;            -- 19
```

## Limitações

- Não é teste de carga: 20 pedidos numa rodada só, sem aquecimento nem repetição. Os tempos servem de referência, não de meta de RNF.
- O shell leva cerca de 3 s para abrir os 20 processos `curl`, então nem todos os pedidos ficaram em voo ao mesmo tempo. A disputa simultânea estrita (latch liberando duas threads juntas) está no teste automatizado `ReservaIntegrationTest.apiAceitaUmaDasDuasReservasSimultaneasParaAMesmaSala`.
- O plano JMeter com 50 usuários (`performance/jmeter`) não foi executado: o JMeter não está instalado na máquina usada. Risco aceito na [decisão de qualidade](../decisao-qualidade.md).
- O usuário Solicitante foi inserido direto no banco local, porque a API não tem cadastro de usuários. A fixture existe só nesse banco descartável.
