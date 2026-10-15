# Plano de recuperação — Semana 42

Situação inicial: a equipe chegou à semana 42 sem implementação consolidada. O objetivo é recuperar os fundamentos em fatias pequenas, mantendo evidências e autoria de João, Miranda e Kauã.

## Ordem de recuperação

### Marco 0 — fundação

- criar repositório público e proteger `main`;
- gerar aplicação Java 21/Spring Boot 3.x com Maven Wrapper;
- definir PostgreSQL, migrations e configuração local;
- registrar integrantes, escopo e comandos no README;
- criar pipeline mínimo com `clean verify`.

### Marco 1 — requisitos e arquitetura

- numerar RFs/RNFs e critérios de aceite;
- criar C4 de contexto e containers/componentes;
- criar utility tree, riscos, trade-offs e ADRs;
- iniciar RTM.

### Marco 2 — catálogo de recursos

- salas, professores e materiais;
- busca e disponibilidade;
- manutenção/bloqueio;
- testes unitários, API e Testcontainers.

### Marco 3 — reservas críticas

- criação/alteração/cancelamento;
- sobreposição de recurso e professor;
- aprovação e estados;
- auditoria;
- concorrência com uma única reserva aceita.

### Marco 4 — qualidade automatizada

- JaCoCo 80/70;
- SonarCloud;
- WireMock para notificação;
- regressão e gestão de defeitos;
- branch protection.

### Marco 5 — semana 42

- escrever RNF mensurável;
- criar JMX parametrizado;
- testar reservas simultâneas;
- diferenciar conflito de negócio esperado de erro técnico;
- registrar p95/p99, throughput, taxa de erro, ambiente e commit;
- atualizar RTM e relatório.

### Marco 6 — fechamento

- relatórios de utilização;
- interface/contrato documentado;
- RTM sem órfãos;
- instalação limpa;
- relatório de qualidade;
- tag de release candidate e roteiro da prova oral.

## Regra de priorização

Primeiro eliminar os quatro bloqueadores: execução, pipeline, autorização e dupla reserva. Depois aumentar cobertura e completar funções secundárias. Nenhuma porcentagem de cobertura compensa um fluxo crítico incorreto.

