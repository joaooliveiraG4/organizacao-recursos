# Escopo e critérios de qualidade

Fonte acadêmica principal: https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html

## Produto

Aplicação Java 21 e Spring Boot 3.x para alocar salas, professores e materiais sem conflitos. Perfis: Solicitante, Responsável e Administrador.

## Requisitos funcionais obrigatórios

- RF-01 — autenticação e autorização por perfil;
- RF-02 — cadastro e consulta de salas, professores e materiais;
- RF-03 — pesquisa por tipo, capacidade, localização, competência e disponibilidade;
- RF-04 — criação, alteração e cancelamento de reservas;
- RF-05 — detecção de sobreposição de sala, material e professor;
- RF-06 — proteção contra dupla reserva em solicitações simultâneas;
- RF-07 — aprovação obrigatória de recursos restritos;
- RF-08 — bloqueio de recursos em manutenção;
- RF-09 — retirada e devolução de materiais/equipamentos;
- RF-10 — histórico auditável de mudanças;
- RF-11 — notificação simulada ou integração externa;
- RF-12 — relatório de utilização, carga horária e conflitos evitados;
- RF-13 — interface responsiva e erros compreensíveis;
- RF-14 — documentação da API ou dos fluxos públicos.

## Regras críticas

- término posterior ao início;
- nenhuma sobreposição do mesmo recurso ou professor;
- duas solicitações simultâneas produzem uma única reserva aceita;
- recurso em manutenção não pode ser reservado;
- fluxo principal: `SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA`;
- estados alternativos: rejeitada, cancelada e não compareceu;
- somente Responsável aprova recurso restrito;
- reserva iniciada não pode ser apagada;
- toda mudança de estado gera auditoria.

## Estratégia obrigatória

- JUnit 5, incluindo testes parametrizados;
- testes unitários, integração, API em caixa-preta e end-to-end;
- Testcontainers para persistência crítica;
- WireMock para integração externa;
- teste automatizado de concorrência;
- evidência de uma funcionalidade desenvolvida por TDD ou BDD;
- GitHub Actions em pull requests;
- JaCoCo e SonarCloud;
- JMeter para desempenho e concorrência;
- validação de entrada, autorização e erros seguros.

## Metas e bloqueadores

- cobertura de linhas >= 80%;
- cobertura de branches >= 70%;
- 100% dos requisitos críticos rastreados;
- zero bugs ou vulnerabilidades críticas;
- bloqueadores de avaliação: aplicação não executa, pipeline ausente, autorização falha ou dupla reserva possível.

## Evidência válida

Uma evidência identifica requisito/caso, ambiente, comando ou execução, resultado e commit. Screenshot isolado, teste sem requisito e resultado de outro commit não fecham a rastreabilidade.

