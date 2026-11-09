# Matriz de Rastreabilidade (RTM)

Atualize esta tabela em toda pull request. Não marque como aprovado sem link para uma execução ligada ao commit correspondente.

| Requisito | Risco principal | Critério/caso | Teste automatizado | Evidência | Status |
|---|---|---|---|---|---|
| RF-01 | acesso com perfil indevido | perfis negam operações não autorizadas | a criar | a criar | pendente |
| RF-02 (salas) | recurso inválido ou duplicado | cadastro/consulta; capacidade positiva; código único sem diferenciar maiúsculas/minúsculas | `SalaServiceTest`, `SalaControllerTest`, `SalaApiIntegrationTest` | [CI da PR #2](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/39631608029): `clean verify` passou no commit `b775487196a5e396b8e1792b6dcfb7a3bdb845c3`; 16 testes passaram, incluindo 3 Testcontainers; JaCoCo aprovou os gates | aprovado no commit validado |
| RF-02 (professores) | professor inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; nome e competência obrigatórios | `ProfessorServiceTest`, `ProfessorControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; merge depende da PR #2 |
| RF-02 (materiais) | material inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; quantidade positiva | `MaterialServiceTest`, `MaterialControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; merge depende da PR #2 |
| RF-03 | disponibilidade incorreta | filtros retornam apenas recursos compatíveis | a criar | a criar | pendente |
| RF-04 | alteração/cancelamento em estado proibido | transições respeitam regras | a criar | a criar | pendente |
| RF-05 | sobreposição de agenda | sala, material e professor não se sobrepõem | a criar | a criar | crítico/pendente |
| RF-06 | dupla reserva concorrente | apenas uma solicitação simultânea é aceita | a criar | a criar | crítico/pendente |
| RF-07 | aprovação por usuário indevido | somente Responsável aprova restrito | a criar | a criar | crítico/pendente |
| RF-08 | manutenção ignorada | recurso bloqueado não pode ser reservado | a criar | a criar | pendente |
| RF-09 | perda de controle de material | retirada/devolução mantém estado coerente | a criar | a criar | pendente |
| RF-10 | mudança sem autoria/histórico | evento com tipo, registro, ação, autor obrigatório (inclusive Sistema) e horário; gravado só na transação da mudança (tudo ou nada); banco recusa UPDATE/DELETE/TRUNCATE; histórico em ordem; sem dados pessoais nos detalhes ([ADR 0003](adr/0003-auditoria-transacional-e-imutavel.md)) | `AuditoriaIntegrationTest` (Testcontainers), `AuditoriaServiceTest` (parametrizado) | local: `mvnw clean verify` com 15 testes passando e 0 pulados, incluindo 7 Testcontainers; JaCoCo aprovado; mutação (`MANDATORY` → `REQUIRED`) reprovou `registrarForaDeUmaTransacaoEhRecusado`. [CI da PR #6](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41657791519): `clean verify` passou no commit `1fcb1065e7850e0d6dc73ce860eb0cafadb6d6a8` | parcial: serviço e garantias prontos; uso nas transições de reserva e endpoint de consulta pendentes |
| RF-11 | provedor externo degrada sistema | timeout/erro gera resposta controlada | a criar | a criar | pendente |
| RF-12 | relatório diverge das reservas | totais conferem com dados persistidos | a criar | a criar | pendente |
| RF-13 | erro inseguro/incompreensível | validações retornam mensagem neutra | a criar | a criar | pendente |
| RF-14 | consumidor não reproduz fluxo | contrato e README permitem execução | a criar | a criar | pendente |
| RNF-QUAL-01 | defeitos, vulnerabilidades ou duplicações chegarem à `main` | toda PR executa testes, cobertura JaCoCo e Quality Gate do SonarCloud | `.github/workflows/quality.yml` e análise automática do SonarCloud | [PR #1: checks e Quality Gate](https://github.com/joaooliveiraG4/organizacao-recursos/pull/1) | aprovado na PR #1 |
