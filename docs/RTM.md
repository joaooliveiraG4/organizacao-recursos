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
| RF-10 | mudança sem autoria/histórico | cada transição gera auditoria | a criar | a criar | pendente |
| RF-11 | provedor externo degrada sistema | timeout/erro gera resposta controlada | a criar | a criar | pendente |
| RF-12 | relatório diverge das reservas | totais conferem com dados persistidos | a criar | a criar | pendente |
| RF-13 | erro inseguro/incompreensível | validações retornam mensagem neutra | a criar | a criar | pendente |
| RF-14 | consumidor não reproduz fluxo | contrato e README permitem execução | a criar | a criar | pendente |
| RF-02 (professores) | professor inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; nome e competência obrigatórios | `ProfessorServiceTest`, `ProfessorControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; merge depende da PR #2 |
| RF-02 (materiais) | material inválido/duplicado ou migration fora de ordem | cadastrar/listar/consultar; código único; quantidade positiva | `MaterialServiceTest`, `MaterialControllerTest`, `ProfessorMaterialApiIntegrationTest` | [CI da PR #5](https://github.com/joaooliveiraG4/organizacao-recursos/actions/runs/41477531535): `clean verify` passou no commit `6d9244b7496a5b09013ffcca8292a1c7f31bc1d0`; 31 testes passaram, incluindo 6 Testcontainers; JaCoCo aprovado | validado em CI; merge depende da PR #2 |
| RNF-QUAL-01 | defeitos, vulnerabilidades ou duplicações chegarem à `main` | toda PR executa testes, cobertura JaCoCo e Quality Gate do SonarCloud | `.github/workflows/quality.yml` e análise automática do SonarCloud | [PR #1: checks e Quality Gate](https://github.com/joaooliveiraG4/organizacao-recursos/pull/1) | aprovado na PR #1 |

## Rastreabilidade arquitetural

Liga cada requisito ao cenário de qualidade da [utility tree](arquitetura/utility-tree.md) e ao componente do [modelo C4](arquitetura/c4.md). Esta seção não muda o status da tabela acima: um requisito só é aprovado com evidência de execução ligada ao commit. A coluna "Natureza" separa o que já foi executado do que é apenas planejado ou proposto.

| Requisito | Cenário na utility tree | Componente C4 | Natureza |
|---|---|---|---|
| RF-01 | Segurança / Autorização; Segurança / Confidencialidade | Segurança | evidência executada no CI da PR #3; ainda fora da `main` |
| RF-02 | Usabilidade / Erros | Catálogo | evidência executada no CI das PRs #2 e #5 |
| RF-04 | — (transições serão cenário de integridade) | Reserva | planejado |
| RF-05 | Integridade / Sobreposição | Reserva | parcial: `PeriodoReservaTest` no CI da PR #4; restrição no banco planejada |
| RF-06 | Integridade / Concorrência | Reserva, PostgreSQL | planejado |
| RF-07 | Segurança / Aprovação | Segurança, Reserva | planejado |
| RF-10 | Auditabilidade / Atomicidade; Auditabilidade / Imutabilidade | Auditoria | evidência executada no CI da PR #6; ainda fora da `main` |
| RF-11 | Disponibilidade / Integração externa | Notificação | **proposta**: limites de 5 s e 2 s a confirmar |
| RF-13 | Usabilidade / Erros | Segurança, Catálogo | parcial: PRs #2, #3 e #5 |
| RF-14 | — | Status não cobre; contratos em `docs/contratos` | pendente |
| RNF-QUAL-01 | Manutenibilidade / Qualidade contínua | Pipeline | parcial: PR #7 aguarda `SONAR_TOKEN` |
| RNF-QUAL-02 | Testabilidade / Ambiente real | Pipeline | evidência executada no CI da PR #7 |
| Desempenho (RNF a definir) | Desempenho / Carga nominal | API, PostgreSQL | **proposta**: 50 usuários, p95 de 500 ms e 1% de erro a confirmar; sem execução |
