# Preparação de QA — utilização, qualidade e concorrência

Responsável: Kauã. Data: 05/11/2026. Base de trabalho: main 8cff3c8.

## O que precisa ser provado

O professor exige histórico de utilização e proteção contra dupla reserva,
além de testes, rastreabilidade e evidências reais. A comparação temporal
da PR #4 é uma parte do fluxo; ela não cria reservas nem protege gravações.

Esta contribuição prepara os cenários e o instrumento de medição. Não implementa
retirada/devolução, serviço de reservas ou notificação. Nenhum requisito abaixo
está concluído com esta preparação.

## RF-09 — retirada e devolução

| Caso | Preparação / ação | Resultado a validar | Situação |
|---|---|---|---|
| U01 | Reserva aprovada de material; registrar retirada | Proposta: APROVADA → EM_USO, horário/autoria e evento na mesma transação | aguarda regra do João e API |
| U02 | Material em uso; registrar devolução | Proposta: EM_USO → CONCLUIDA, horário/autoria e evento na mesma transação | aguarda regra do João e API |
| U03 | Retirar reserva solicitada, cancelada ou concluída | Recusa sem alterar reserva/material/auditoria | regra exata pendente |
| U04 | Repetir retirada/devolução | Não duplicar movimentação; decidir erro versus idempotência | decisão pendente |
| U05 | Perfil sem permissão | 403, nenhuma alteração | matriz de RF-09 pendente |
| U06 | Falha ao salvar movimento ou auditoria | Rollback integral; nenhuma alteração parcial | depende da PR #6 integrada |
| U07 | Duas retiradas simultâneas da mesma reserva | Uma única movimentação confirmada | API/banco pendentes |

A especificação informa a cadeia SOLICITADA → APROVADA → EM_USO → CONCLUIDA
e diz que o Responsável acompanha retiradas/devoluções. Ela não define sozinha
todos os endpoints, autorizações e a política de repetição. Confirmar com João
antes de transformar as propostas da tabela em código.

Quantidade no catálogo é o total cadastrado; não deve ser reduzida automaticamente
como se fosse estoque vendido. Definir com a equipe a unidade reservável,
quantidade por empréstimo e controle de disponibilidade antes de alterar o catálogo.

## RF-06 — disputa de reservas

Dado recurso/horário livre, quando N usuários tentam a mesma reserva juntos,
uma deve ser aceita e N-1 recusadas por conflito, sem 500 e sem duplicidade
no PostgreSQL. O plano JMeter está em performance/jmeter/disputa-reserva.jmx;
procedimento e limitações em performance/README.md.

Cenários de regressão: período consecutivo permitido; interseção recusada;
recurso diferente não conflita; professor compartilhado conflita; material em
manutenção recusado; alteração respeita conflitos; cancelamento respeita a
política aprovada; transição e auditoria são atômicas. Todos dependem da API
e não foram executados nesta contribuição.

## Marco 4 — qualidade automatizada

- CI/Sonar: reutilizar a PR #7 do João, ainda aberta na consulta de 05/11.
  Não duplicar o workflow nem confundir aprovação da PR com integração.
- Confirmar token do Sonar e importação de cobertura após sua configuração.
- Não aceitar testes críticos ignorados como prova. A PR #7 adiciona teste estrito.
- WireMock/notificação: aguarda contrato do provedor, evento que dispara,
  payload, timeout e política de falha/repetição. Não criar integração fictícia.
- Proteção da branch: confirmar os checks obrigatórios após integração; não
  modificar permissões/configurações administrativas nesta contribuição.
- Registro de defeitos: usar a estrutura abaixo com evidência vinculada ao commit.

## Registro de defeitos

Para cada falha real: identificador, RF, commit, ambiente, pré-condições,
passos, resultado esperado, observado, severidade, responsável, teste de
regressão e evidência da correção. Não abrir defeito baseado apenas em hipótese.

## Marco 5 — critério mensurável

RNF de integridade: em cada disputa controlada sobre recurso/horário inicialmente
livre, exatamente uma reserva persistida, N-1 conflitos esperados e zero erros
técnicos. Verificar por HTTP e pelo banco. Esse critério decorre da regra crítica
do projeto. Tempos, volume de carga nominal e duração precisam ser aprovados.

Relatar p95/p99, throughput, erro técnico, conflitos esperados, amostras,
ambiente e commit. Não publicar resultados fictícios. Modelo em
[relatorio-carga-modelo.md](relatorio-carga-modelo.md).

## Bloqueios constatados

- PRs #2 (salas), #3 (perfis), #6 (auditoria) e #7 (CI) ainda abertas na consulta.
- Main ainda sem endpoint de reservas e suas transições.
- Docker e JMeter não encontrados na máquina.
- Regras específicas de RF-09 e contratos de reservas/notificação precisam de alinhamento.
- Git fetch desta sessão falhou por rede; referências atualizadas pelo PowerShell do Kauã.

Fonte: [especificação do professor](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html).

## Verificação desta preparação — 05/11/2026

- Avaliador: 12 testes próprios aprovados, com fixtures sintéticas.
- JMX: XML bem formado e estrutura revisada; não carregado nem executado no JMeter.
- Maven Wrapper, Java 21, modo offline com dependências já disponíveis:
  build e gate JaCoCo aprovados; 31 casos encontrados, **25 aprovados e 6 pulados**.
  Os 6 pulados são de ProfessorMaterialApiIntegrationTest por falta de Docker.
  Não considerar essa execução aprovação da integração crítica.
- A primeira tentativa Java travou durante autoanexação do Mockito, com acesso
  negado ao pipe. Foi interrompida. A repetição usou o Mockito 5.17.0 já no cache
  como javaagent via JAVA_TOOL_OPTIONS apenas no processo; não houve alteração
  de Java global, permissões, pom.xml ou limites de cobertura.
- Avisos restantes: CDS/boot classpath, autenticação padrão da base e Docker ausente.
  Não publicar o log bruto, que pode conter credenciais temporárias geradas pelo Spring.
- CI recebe um passo para os testes do avaliador. A execução remota ligada ao
  commit ainda precisa ser confirmada na PR.
- Não houve teste real de retirada/devolução, notificação, concorrência ou carga.
