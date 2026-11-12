# Decisão de qualidade — `v1.0.0-rc2`

Data: 12/11/2026. Grupo: João, Miranda e Kauã.

## Decisão: **liberar com ressalvas**

A versão cumpre os 14 requisitos funcionais. Cada um tem teste automatizado rodando contra PostgreSQL real e evidência no CI do commit da tag. Duas ressalvas impedem chamar a versão de "pronta para produção":

- sob disputa de 50 usuários pela mesma sala, a integridade se manteve, mas o p95 ficou entre 1,1 s e 1,5 s, acima da meta proposta de 500 ms;
- as evidências vêm de CI e de uma máquina local, não de um ambiente parecido com produção.

## Critérios e evidências

| # | Critério | Meta | Medido | Evidência | Atende |
|---|---|---|---|---|---|
| C1 | Suíte completa verde no commit da tag | 0 falhas, 0 pulados | 225 testes (inclui RF-05 da PR #22 e a regressão do D-09), 0 pulados | [CI da tag](evidencias/README.md) | sim |
| C2 | Cobertura de linhas (JaCoCo) | ≥ 80% | 98,5% | artefato `relatorios-N` do CI; [relatório](relatorio-qualidade.md) | sim |
| C3 | Cobertura de branches (JaCoCo) | ≥ 70% | 91,7% | idem | sim |
| C4 | Quality Gate do SonarCloud | aprovado, sem vulnerabilidade aberta | aprovado; S2077 corrigido; S4502 aceito com ADR | [SonarCloud](https://sonarcloud.io/project/overview?id=joaovictordasilvaolveira_organizacao-recursos), [ADR 0006](adr/0006-csrf-em-api-stateless.md) | sim |
| C5 | Rastreabilidade | todo RF com risco, teste e evidência | RF-01 a RF-14 | [RTM](rtm.md) | sim |
| C6 | Integridade sob disputa (RF-06) | 1 reserva aceita, demais `409`, 0 erro técnico | JMeter, 50 usuários × 3 rodadas: 1 `201`, 49 `409`, 0 erro, 1 alocação em cada | [resultado-02](performance/resultado-02.md), [resultado-01](performance/resultado-01.md); `ReservaIntegrationTest` | sim |
| C7 | Testes detectam defeito | mutação reprova o teste | 7 mutações manuais reprovaram | [relatório](relatorio-qualidade.md), [RTM](rtm.md) | sim |
| C8 | Reprodutível por terceiro | clone limpo + README = build verde e app no ar | sim, 12/11/2026 | [checklist](entrega/checklist.md) | sim |
| C9 | Desempenho sob carga nominal | p95 ≤ 500 ms com 50 usuários | p95 de 1 119 a 1 459 ms | [resultado-02](performance/resultado-02.md) | **não** (risco R1) |

## Riscos aceitos

| ID | Risco | Impacto | Justificativa | Dono |
|---|---|---|---|---|
| R1 | p95 de 1,1–1,5 s com 50 usuários disputando a mesma sala | quem disputa o mesmo recurso espera mais de 1 s pela resposta | é o pior caso (todos na mesma trava, atendidos um de cada vez); não houve erro nem dado errado, e a meta de 500 ms nunca foi aprovada como RNF. Carga em recursos diferentes fica para a próxima versão | Kauã |
| R2 | Notificação sem reenvio (sem outbox) | se o provedor estiver fora do ar, o aviso se perde; a reserva continua correta | a falha é registrada e não desfaz a reserva; reenvio fica para a próxima versão | João |
| R3 | CSRF desligado | só vira risco se surgir interface web que guarde credenciais no navegador | API stateless com HTTP Basic, sem cookie ([ADR 0006](adr/0006-csrf-em-api-stateless.md)) | João |
| R4 | Reserva já existente não é cancelada quando o recurso entra em manutenção | alguém pode ter reserva de recurso em manutenção | só reservas novas e editadas são bloqueadas; o Responsável trata as antigas manualmente | João |
| R5 | Material reservado por inteiro, sem controle de quantidade | material com várias unidades fica bloqueado por uma só reserva | decisão de escopo ([ADR 0005](adr/0005-reserva-integral-de-materiais.md)) | Kauã |
| R6 | Pesquisa de catálogo não filtra por horário livre | quem pesquisa só descobre o conflito ao reservar (`409`) | a regra de disponibilidade é garantida na reserva (RF-05/06) | Miranda |
| R7 | Usuários só entram pelo banco ou pelo Administrador inicial | cadastro de usuários exige acesso técnico | fora do escopo dos 14 RFs | João |

## Declaração dos integrantes

Cada integrante confirma o texto da própria declaração ao aprovar a PR desta entrega.

- **João** (integração e qualidade): fiz autenticação e perfis (RF-01), auditoria (RF-10), manutenção (RF-08), retirada e devolução (RF-09), notificação (RF-11), relatório de utilização (RF-12), o modo estrito do Testcontainers, os diagramas C4, a configuração do SonarCloud e a correção do S2077. Integrei as PRs, mantive a RTM e preparei esta release candidate.
- **Miranda** (catálogo): fiz o cadastro e a consulta de salas, professores e materiais (RF-02) e os filtros de pesquisa (RF-03), incluindo a correção dos filtros ausentes no PostgreSQL. Registrei as evidências de CI do catálogo na RTM.
- **Kauã** (reservas e QA): fiz o modelo de reserva e a migration V6 com a restrição contra sobreposição (RF-05/06), criação, edição e cancelamento (RF-04), aprovação pelo Responsável (RF-07), o roteiro de QA de utilização e concorrência e o plano JMeter de disputa. Revisei a notificação e encontrei o defeito do `3xx` (D-02).
