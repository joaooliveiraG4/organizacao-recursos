# Matriz de Rastreabilidade (RTM)

Atualize esta tabela em toda pull request. Não marque como aprovado sem link para uma execução ligada ao commit correspondente.

| Requisito | Risco principal | Critério/caso | Teste automatizado | Evidência | Status |
|---|---|---|---|---|---|
| RF-01 | acesso com perfil indevido | negar por padrão; 401 sem identidade confirmada (mesma resposta para senha errada, e-mail inexistente e inativo); 403 para perfil sem permissão; escrita no catálogo só por Administrador; senha em BCrypt ([ADR 0002](adr/0002-autenticacao-e-perfis.md)) | `AutorizacaoPorPerfilTest` (matriz parametrizada), `AutenticacaoIntegrationTest` (Testcontainers), `UsuarioDetailsServiceTest`, `AdministradorInicialTest` | local: `mvnw clean verify` com 37 testes passando e 0 pulados, incluindo 3 Testcontainers; JaCoCo aprovado; mutação (catálogo aberto a qualquer perfil) reprovou 6 casos da matriz. CI: pendente | aguardando CI |
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
| RNF-QUAL-01 | defeitos, vulnerabilidades ou duplicações chegarem à `main` | toda PR executa testes, cobertura JaCoCo e Quality Gate do SonarCloud | `.github/workflows/quality.yml` e análise automática do SonarCloud | [PR #1: checks e Quality Gate](https://github.com/joaooliveiraG4/organizacao-recursos/pull/1) | aprovado na PR #1 |

