# Resultado 02 — disputa com JMeter, 50 usuários (RF-06)

Status: **executado em 12/11/2026, 18:24 (BRT, UTC-3)**. Aprovado depois da correção do defeito D-09.

Plano: [`performance/jmeter/disputa-reserva.jmx`](../../performance/jmeter/disputa-reserva.jmx) (Kauã, PR #9), sem alteração. Cada rodada passou pelo avaliador [`performance/avaliar-disputa.ps1`](../../performance/avaliar-disputa.ps1). Ele só aprova com exatamente 1 `201`, N-1 `409`, nenhuma amostra em falha e todas as tentativas sobrepostas no tempo.

## Ambiente

| Item | Valor |
|---|---|
| Aplicação | commit `b87e347c5ba202d97e129d2a318eb38aec045dd4` (correção do D-09), clone limpo, `mvnw spring-boot:run` |
| Banco | PostgreSQL 17.11 do `compose.yaml`, migrations V1–V9 |
| Java / JMeter | OpenJDK 21.0.7 / Apache JMeter 5.6.3 (zip oficial, SHA-512 conferido) |
| Topologia | cliente JMeter, API e banco na mesma máquina (notebook Windows 11) |
| Pool de conexões | padrão do Spring Boot (HikariCP, 10 conexões) |
| Autenticação | Solicitante sintético `qa-solic@exemplo.test`; credencial só na variável `RECURSOS_QA_AUTHORIZATION` da sessão |
| Fixture | sala `QA-JMETER` sem alocações; um dia livre diferente por rodada (06 a 09/12/2026, 13h–15h UTC) |

## Comando (sem credenciais)

```powershell
jmeter -n -q performance/jmeter/qa.properties -t performance/jmeter/disputa-reserva.jmx `
  "-JbaseUrl=http://localhost:8080" "-JreservaPath=/api/v1/reservas" `
  "-JpayloadFile=<caminho>\payload-N.json" "-Jusers=50" `
  -l performance/resultados/disputa-N.jtl -j performance/resultados/disputa-N.log
.\performance\avaliar-disputa.ps1 -Jtl performance/resultados/disputa-N.jtl -Usuarios 50
```

No PowerShell 5.1 os `-J` precisam de aspas. Sem elas, `-JbaseUrl=http://...` é cortado no `:` e o plano recusa a configuração.

## Resultado

| Rodada | Usuários | `201` | `409` | Erros técnicos | p95 | p99 | Vazão | Diferença entre inícios |
|---|---|---|---|---|---|---|---|---|
| 1 (calibração) | 2 | 1 | 1 | 0 | 697 ms | 697 ms | 2,9 req/s | 0 ms |
| 2 | 50 | 1 | 49 | 0 | 1 459 ms | 1 479 ms | 33,8 req/s | 2 ms |
| 3 | 50 | 1 | 49 | 0 | 1 306 ms | 1 312 ms | 38,0 req/s | 7 ms |
| 4 | 50 | 1 | 49 | 0 | 1 119 ms | 1 129 ms | 44,2 req/s | 1 ms |

Conferência no banco, sem alterar dados:

| Rodada | Alocações ativas | `conflito_evitado` | Auditoria `RESERVA_CRIADA` |
|---|---|---|---|
| 1 | 1 | 1 | 1 |
| 2 | 1 | 49 | 1 |
| 3 | 1 | 49 | 1 |
| 4 | 1 | 49 | 1 |

Log da aplicação sem nenhum `Connection is not available`.

## Antes da correção (defeito D-09)

Mesmo plano, mesma máquina, commit `ae5f25c` (antes da correção):

- rodada de 50 usuários: 1 `201` e **49 timeouts** de 10 s no cliente;
- rodadas seguintes: **50 timeouts**;
- log: `HikariPool-1 - Connection is not available ... (total=10, active=10, idle=0, waiting=98)`.

Cada pedido perdedor segurava a conexão da reserva e pedia uma segunda conexão (`REQUIRES_NEW`) para gravar o conflito. Com 10 perdedores ao mesmo tempo, o pool esgotava. Os dados nunca ficaram errados: cada rodada gravou 1 reserva. Correção e teste de regressão no [registro de defeitos](../defeitos/README.md).

## Conclusão

- **Integridade (RF-06): aprovada.** Em 3 rodadas de 50 usuários simultâneos, exatamente 1 reserva foi gravada, os 49 perdedores receberam `409` com o conflito registrado, e não houve erro técnico.
- **Latência: acima da meta proposta.** O p95 ficou entre 1,1 s e 1,5 s, acima dos 500 ms propostos na utility tree, que nunca foram aprovados como RNF. É esperado neste cenário: os 50 pedidos disputam a trava da mesma sala e são atendidos um de cada vez. Risco aceito na [decisão de qualidade](../decisao-qualidade.md).
- Limitação: cliente e servidor na mesma máquina, poucas rodadas e sem aquecimento separado. Os números valem para este cenário de disputa, não como capacidade do sistema.
