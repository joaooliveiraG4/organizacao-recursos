# Matriz de Rastreabilidade (RTM)

Atualize esta tabela em toda pull request. Não marque como aprovado sem link para uma execução ligada ao commit correspondente.

| Requisito | Risco principal | Critério/caso | Teste automatizado | Evidência | Status |
|---|---|---|---|---|---|
| RF-01 | acesso com perfil indevido | negar por padrão; 401 sem identidade confirmada (mesma resposta para senha errada, e-mail inexistente e inativo); 403 para perfil sem permissão; escrita no catálogo só por Administrador; senha em BCrypt ([ADR 0002](adr/0002-autenticacao-e-perfis.md)) | `AutorizacaoPorPerfilTest` (matriz parametrizada), `AutenticacaoIntegrationTest` (Testcontainers), `UsuarioDetailsServiceTest`, `AdministradorInicialTest` | [CI da PR #3 no commit `a79e0b9612bbf8b1cbd797596c95421380df66ff`](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/42708381599): `clean verify` passou com 110 testes, 0 falhas e 0 pulados | aprovado no commit validado |
| RF-02 (salas) | recurso inválido ou duplicado | cadastro/consulta; capacidade positiva; código único sem diferenciar maiúsculas/minúsculas | `SalaServiceTest`, `SalaControllerTest`, `SalaApiIntegrationTest` | [CI da PR #2](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/39631608029): `clean verify` passou no commit `b775487196a5e396b8e1792b6dcfb7a3bdb845c3`; 16 testes passaram, incluindo 3 Testcontainers; JaCoCo aprovou os gates | aprovado no commit validado |
| RF-02 (professores) | professor inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; nome e competência obrigatórios | `ProfessorServiceTest`, `ProfessorControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; na `main` |
| RF-02 (materiais) | material inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; quantidade positiva | `MaterialServiceTest`, `MaterialControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; na `main` |
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
| RNF-QUAL-01 | defeitos, vulnerabilidades ou duplicações chegarem à `main` | toda PR executa testes, cobertura JaCoCo e Quality Gate do SonarCloud **com a cobertura importada** ([ADR 0004](adr/0004-sonarcloud-pelo-ci.md)) | `.github/workflows/quality.yml` (passo `sonar:sonar` após `clean verify`) | [PR #1: checks e Quality Gate](https://github.com/joaooliveiraG4/organizacao-recursos/pull/1). Lacuna: nas PRs #3 e #6 o Sonar mostrou 0% de cobertura (análise automática). Correção aguardando `SONAR_TOKEN` e desativação da análise automática | parcial: gate sem cobertura até a ativação |
| RNF-QUAL-02 | testes de integração pulados em silêncio deixam o build verde sem provar persistência | Testcontainers 1.21.4 detecta Docker Desktop recente sem contorno; teste de fumaça estrito (sem `disabledWithoutDocker`) aplica todas as migrations no PostgreSQL real e reprova a suíte se o Docker faltar | `MigracoesPostgresIntegrationTest` | [CI da PR #7 no commit `fc356faf053e9f25673367d92be0ce3fbf28f99a`](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/42710441099): `clean verify` passou com 111 testes e 0 pulados | aprovado no commit validado |

## Rastreabilidade arquitetural

Liga cada requisito ao cenário de qualidade da [utility tree](arquitetura/utility-tree.md) e ao componente do [modelo C4](arquitetura/c4.md). Esta seção não muda o status da tabela acima: um requisito só é aprovado com evidência de execução ligada ao commit. A coluna "Natureza" separa o que já foi executado do que é apenas planejado ou proposto.

| Requisito | Cenário na utility tree | Componente C4 | Natureza |
|---|---|---|---|
| RF-01 | Segurança / Autorização; Segurança / Confidencialidade | Segurança | evidência executada no CI da PR #3; na `main` |
| RF-02 | Usabilidade / Erros | Catálogo | evidência executada no CI das PRs #2 e #5 |
| RF-04 | — (transições serão cenário de integridade) | Reserva | planejado |
| RF-05 | Integridade / Sobreposição | Reserva | parcial: `PeriodoReservaTest` no CI da PR #4; restrição no banco planejada |
| RF-06 | Integridade / Concorrência | Reserva, PostgreSQL | planejado |
| RF-07 | Segurança / Aprovação | Segurança, Reserva | planejado |
| RF-10 | Auditabilidade / Atomicidade; Auditabilidade / Imutabilidade | Auditoria | evidência executada no CI da PR #6; na `main` |
| RF-11 | Disponibilidade / Integração externa | Notificação | **proposta**: limites de 5 s e 2 s a confirmar |
| RF-13 | Usabilidade / Erros | Segurança, Catálogo | parcial: PRs #2, #3 e #5 |
| RF-14 | — | Status não cobre; contratos em `docs/contratos` | pendente |
| RNF-QUAL-01 | Manutenibilidade / Qualidade contínua | Pipeline | parcial: PR #7 aguarda `SONAR_TOKEN` |
| RNF-QUAL-02 | Testabilidade / Ambiente real | Pipeline | evidência executada no CI da PR #7; na `main` |
| Desempenho (RNF a definir) | Desempenho / Carga nominal | API, PostgreSQL | **proposta**: 50 usuários, p95 de 500 ms e 1% de erro a confirmar; sem execução |
