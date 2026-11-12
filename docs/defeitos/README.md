# Registro de defeitos

Defeitos reais encontrados durante o desenvolvimento, com a causa, a correção e o teste que impede a volta. Todos estão corrigidos na `main`.

| ID | Requisito | Defeito | Como foi achado | Causa | Correção | Teste de regressão | Status |
|---|---|---|---|---|---|---|---|
| D-01 | RF-03 | `capacidadeMinima` 0 ou negativa era aceita e respondia `200` | teste escrito antes da correção (TDD) | parâmetro sem validação | commit `42083fd`: recusa com `400` | `SalaControllerTest`, `SalaApiIntegrationTest` | corrigido |
| D-02 | RF-11 | provedor de notificação com redirecionamento (`301`/`302`) contava como "enviada" | revisão do Kauã na PR #13 | `HttpClient` tratava toda resposta não-erro como sucesso | commit `a408ae0`: só `2xx` conta como enviada | `ClienteNotificacaoTest` (301, 302, 400, 500, 503) | corrigido |
| D-03 | RF-11 | chamada ao provedor falhava contra servidor só HTTP/1.1 | teste com WireMock | `HttpClient` do JDK tenta HTTP/2 por padrão | cliente fixado em `HTTP_1_1` | `ClienteNotificacaoTest` | corrigido |
| D-04 | RF-12 | relatório de utilização quebrava com erro do PostgreSQL "column reference codigo is ambiguous" | `RelatorioUtilizacaoIntegrationTest` com banco real | `GROUP BY codigo` com três tabelas que têm `codigo` | agrupamento pelas colunas qualificadas (`s.codigo`, `p.codigo`, `m.codigo`) | `RelatorioUtilizacaoIntegrationTest` | corrigido |
| D-05 | segurança | SQL de travamento e manutenção montado por concatenação com o nome da tabela | SonarCloud `java:S2077` (Security Rating D na `main`) | nome da tabela vinha do enum e era concatenado | PR #21 (`7e4c33a`): consultas fixas por tipo | `ReservaIntegrationTest`, `ManutencaoIntegrationTest`; Quality Gate | corrigido |
| D-06 | RNF-QUAL-02 | testes de integração pulavam sem Docker e o build ficava verde | revisão da PR #7 | `@Testcontainers(disabledWithoutDocker = true)` | modo estrito: sem Docker a suíte reprova | `MigracoesPostgresIntegrationTest` | corrigido |
| D-07 | teste | teste de timeout do RF-11 oscilava com o JaCoCo ligado | CI intermitente | primeira chamada incluía o aquecimento da JVM | chamada de aquecimento antes de medir | `ClienteNotificacaoTest` | corrigido |
| D-08 | RF-03 | pesquisa de catálogo sem filtro quebrava no PostgreSQL | teste de integração com banco real (Miranda) | parâmetro nulo sem tipo na consulta | correção "handle absent catalog filters in PostgreSQL" | `SalaApiIntegrationTest`, `ProfessorMaterialApiIntegrationTest` | corrigido |

## Como registrar um novo defeito

1. Escreva o teste que reproduz o defeito e confirme que ele **reprova**.
2. Corrija a causa e confirme que o teste passa.
3. Acrescente uma linha nesta tabela e cite o teste na [RTM](../rtm.md).
