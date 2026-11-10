# Utility tree, riscos e trade-offs

Cenários de atributo de qualidade no formato ATAM: estímulo → resposta mensurável. A prioridade usa (importância para o negócio, dificuldade técnica), em A (alta), M (média) e B (baixa). Cenários (A, A) são os que mais pesam na avaliação.

Os limites numéricos de disponibilidade e desempenho (5 s, 2 s, 50 usuários, p95 de 500 ms, 1% de erro) são **propostas da equipe a confirmar**, não vêm da especificação. Eles precisam ser fechados antes do cenário JMeter (Marco 5), junto com o ambiente de medição.

## Utility tree

| Atributo | Refinamento | Cenário | Prioridade | Requisito | Como é verificado |
|---|---|---|---|---|---|
| Integridade | Concorrência | Duas solicitações simultâneas para a mesma sala e horário: exatamente uma é aceita; a outra recebe 409, sem erro 500 e sem linha duplicada | (A, A) | RF-06 | teste de concorrência com Testcontainers e cenário JMeter (planejados) |
| Integridade | Sobreposição | Uma reserva das 15h às 17h é recusada se a sala já está reservada das 14h às 16h; uma das 16h às 17h é aceita | (A, M) | RF-05 | `PeriodoReservaTest` (PR #4); restrição no banco planejada |
| Segurança | Autorização | Um Solicitante autenticado que tenta alterar o catálogo recebe 403 em 100% das rotas de escrita | (A, M) | RF-01 | `AutorizacaoPorPerfilTest`, matriz parametrizada (PR #3) |
| Segurança | Confidencialidade | Senha errada, e-mail inexistente e usuário inativo recebem a mesma resposta 401 | (A, B) | RF-01, RF-13 | `AutenticacaoIntegrationTest` (PR #3) |
| Segurança | Aprovação | Só o Responsável aprova recurso restrito; outro perfil recebe 403 | (A, M) | RF-07 | planejado |
| Auditabilidade | Atomicidade | Se a mudança de estado falhar, nenhum evento de auditoria fica gravado; se a auditoria falhar, a mudança é desfeita | (A, M) | RF-10 | `AuditoriaIntegrationTest` (PR #6) |
| Auditabilidade | Imutabilidade | `UPDATE`, `DELETE` ou `TRUNCATE` em `audit_event` são recusados pelo banco | (A, B) | RF-10 | `AuditoriaIntegrationTest` (PR #6) |
| Disponibilidade | Integração externa | Com o serviço de notificação em timeout de 5 s, a aprovação conclui e o usuário recebe resposta em até 2 s | (M, M) | RF-11 | WireMock (planejado) |
| Desempenho | Carga nominal | 50 usuários simultâneos consultando disponibilidade: p95 abaixo de 500 ms e taxa de erro abaixo de 1% | (M, M) | RNF de desempenho | JMeter (planejado) |
| Testabilidade | Tempo controlado | Toda regra que depende de horário usa `Clock` injetado e pode ser testada com horário fixo | (M, B) | todos | `StatusControllerTest`, `AuditoriaServiceTest` |
| Testabilidade | Ambiente real | A suíte reprova se o PostgreSQL não puder ser iniciado; teste pulado não conta como aprovado | (M, B) | RNF-QUAL-02 | `MigracoesPostgresIntegrationTest` |
| Manutenibilidade | Qualidade contínua | Toda PR passa por testes, JaCoCo (80% linhas / 70% branches) e Quality Gate com cobertura | (M, B) | RNF-QUAL-01 | `.github/workflows/quality.yml`, [ADR 0004](../adr/0004-sonarcloud-pelo-ci.md) |
| Usabilidade | Erros | Dado inválido retorna 400 com o campo e a regra violada, sem stack trace | (M, B) | RF-13 | testes de controller das PRs #2 e #5 |

## Riscos

| Risco | Cenário afetado | Situação | Mitigação |
|---|---|---|---|
| Dupla reserva aceita sob concorrência | Integridade / concorrência | **aberto, crítico** | exclusion constraint no PostgreSQL e teste com requisições simultâneas (Kauã) |
| Aprovação de restrito sem perfil Responsável | Segurança / aprovação | aberto | `@PreAuthorize` com base na PR #3 |
| Força bruta no login | Segurança | aberto, aceito para a entrega | registrado no ADR 0002; limite de tentativas fora do escopo |
| Gate do Sonar sem cobertura | Manutenibilidade | em correção | ADR 0004 aguarda token |
| Dono da tabela desativa o trigger de auditoria | Auditabilidade | aceito | usuário de banco da aplicação sem privilégio de dono em produção |

## Pontos de sensibilidade e trade-offs

- **Regra no banco × regra no código (RF-05/06).** Checar conflito só no serviço é mais simples, mas duas transações simultâneas passam pela checagem ao mesmo tempo. A restrição no PostgreSQL é a única que garante uma reserva aceita; o custo é depender de recurso específico do PostgreSQL (o que o ADR 0001 já aceitou ao rejeitar H2).
- **Atomicidade × disponibilidade (RF-10).** Exigir auditoria na mesma transação faz a operação falhar quando a auditoria falha. Foi escolhido porque mudança sem histórico viola a regra crítica; na prática, a auditoria só falha quando o banco falha, e aí a mudança falharia de qualquer forma.
- **HTTP Basic × tokens (RF-01).** Basic é simples de testar e demonstrar; exige HTTPS e envia a credencial em toda requisição. A troca por tokens não muda a matriz de perfis.
- **Notificação depois do commit (RF-11).** O usuário não espera o provedor externo e uma falha nele não desfaz a decisão; o custo é a notificação poder atrasar ou precisar de nova tentativa.
