# Matriz de Rastreabilidade (RTM)

Atualize esta tabela em toda pull request. Não marque como aprovado sem link para uma execução ligada ao commit correspondente.

| Requisito | Risco principal | Critério/caso | Teste automatizado | Evidência | Status |
|---|---|---|---|---|---|
| RF-01 | acesso com perfil indevido | perfis negam operações não autorizadas | a criar | a criar | pendente |
| RF-02 | recurso inválido ou duplicado | cadastro e consulta preservam invariantes | a criar | a criar | pendente |
| RF-03 | disponibilidade incorreta | filtros retornam apenas recursos compatíveis | a criar | a criar | pendente |
| RF-04 | alteração/cancelamento em estado proibido | transições respeitam regras | a criar | a criar | pendente |
| RF-05 | sobreposição de agenda | sala, material e professor não se sobrepõem | a criar | a criar | crítico/pendente |
| RF-06 | dupla reserva concorrente | apenas uma solicitação simultânea é aceita | a criar | a criar | crítico/pendente |
| RF-07 | aprovação por usuário indevido | somente Responsável aprova restrito | a criar | a criar | crítico/pendente |
| RF-08 | manutenção ignorada | recurso bloqueado não pode ser reservado | a criar | a criar | pendente |
| RF-09 | perda de controle de material | retirada/devolução mantém estado coerente | a criar | a criar | pendente |
| RF-10 | mudança sem autoria/histórico | evento com tipo, registro, ação, autor obrigatório (inclusive Sistema) e horário; gravado só na transação da mudança (tudo ou nada); banco recusa UPDATE/DELETE/TRUNCATE; histórico em ordem; sem dados pessoais nos detalhes ([ADR 0003](adr/0003-auditoria-transacional-e-imutavel.md)) | `AuditoriaIntegrationTest` (Testcontainers), `AuditoriaServiceTest` (parametrizado) | local: `mvnw clean verify` com 15 testes passando e 0 pulados, incluindo 7 Testcontainers; JaCoCo aprovado; mutação (`MANDATORY` → `REQUIRED`) reprovou `registrarForaDeUmaTransacaoEhRecusado`. CI: pendente | parcial: serviço e garantias prontos; uso nas transições de reserva e endpoint de consulta pendentes |
| RF-11 | provedor externo degrada sistema | timeout/erro gera resposta controlada | a criar | a criar | pendente |
| RF-12 | relatório diverge das reservas | totais conferem com dados persistidos | a criar | a criar | pendente |
| RF-13 | erro inseguro/incompreensível | validações retornam mensagem neutra | a criar | a criar | pendente |
| RF-14 | consumidor não reproduz fluxo | contrato e README permitem execução | a criar | a criar | pendente |
| RNF-QUAL-01 | defeitos, vulnerabilidades ou duplicações chegarem à `main` | toda PR executa testes, cobertura JaCoCo e Quality Gate do SonarCloud | `.github/workflows/quality.yml` e análise automática do SonarCloud | [PR #1: checks e Quality Gate](https://github.com/joaooliveiraG4/organizacao-recursos/pull/1) | aprovado na PR #1 |

