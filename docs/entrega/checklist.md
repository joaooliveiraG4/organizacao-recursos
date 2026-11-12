# Checklist de entrega — clone limpo

Executado em **12/11/2026** por João, como avaliador externo:

1. `git clone https://github.com/joaovictordasilvaolveira/organizacao-recursos.git` num diretório temporário vazio, sem reaproveitar nada da máquina de desenvolvimento (commit `c30650735b80ce13f0fa58caac4bf2bb64f579ff`).
2. Só os passos do README: `Copy-Item .env.example .env`, `docker compose up -d`, `.\mvnw.cmd -B clean verify`, `.\mvnw.cmd spring-boot:run`.
3. Resultado: `clean verify` com **BUILD SUCCESS**, **222 testes, 0 falhas, 0 erros, 0 pulados** (4 min 17 s). Aplicação subiu; `/api/v1/status` e `/actuator/health` responderam `UP`.

Ambiente: Windows 11, OpenJDK 21.0.7, Docker Desktop 29.8 (PostgreSQL 17 pelo `compose.yaml` e pelo Testcontainers).

## Itens

| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório revisado por pares | sim | sim: as mudanças entraram por PR (#2 a #24) com revisão de outro integrante | [pull requests](https://github.com/joaovictordasilvaolveira/organizacao-recursos/pulls?q=is%3Apr+is%3Amerged) |
| Aplicação executável | sim | sim: subiu no clone limpo | `src/`, `compose.yaml`, [README](../../README.md) |
| README | sim | sim: passos reproduzidos sem ajuste | [README.md](../../README.md) |
| Documentação da API | sim | sim | [docs/contratos/](../contratos/) |
| RTM | sim | sim: RF-01 a RF-14 com teste e evidência | [docs/rtm.md](../rtm.md) |
| Diagrama de contexto | sim | sim | [docs/arquitetura/c4.md](../arquitetura/c4.md) (nível 1) |
| Diagrama de componentes | sim | sim | [docs/arquitetura/c4.md](../arquitetura/c4.md) (níveis 2 e 3) |
| Diagrama de sequência crítica | sim | criado nesta entrega | [docs/arquitetura/sequencia-reserva.md](../arquitetura/sequencia-reserva.md) |
| Plano de testes | sim | sim | [docs/testes/plano-de-testes.md](../testes/plano-de-testes.md) |
| Relatório de testes | sim | sim | [docs/relatorio-qualidade.md](../relatorio-qualidade.md); `target/surefire-reports` e `target/failsafe-reports` no artefato do CI |
| Pipeline de CI | sim | sim: roda em PR, na `main` e em tags `v*` | [.github/workflows/quality.yml](../../.github/workflows/quality.yml) |
| Relatório SonarCloud | sim | sim: Quality Gate aprovado na `main` | [painel SonarCloud](https://sonarcloud.io/project/overview?id=joaovictordasilvaolveira_organizacao-recursos) |
| Relatório JaCoCo | sim | sim | artefato `relatorios-N` do CI; local em `target/site/jacoco/index.html` |
| Plano de teste de carga | sim | sim | [performance/README.md](../../performance/README.md), [performance/jmeter/disputa-reserva.jmx](../../performance/jmeter/disputa-reserva.jmx) |
| Resultado de teste de carga | parcial | criado nesta entrega: disputa real com 20 pedidos; carga JMeter de 50 usuários não executada | [docs/performance/resultado-01.md](../performance/resultado-01.md) |
| Registro de defeitos | sim | criado nesta entrega | [docs/defeitos/](../defeitos/README.md) |
| Relatório final de qualidade | sim | sim | [docs/relatorio-qualidade.md](../relatorio-qualidade.md) |
| Índice de evidências | sim | criado nesta entrega | [docs/evidencias/README.md](../evidencias/README.md) |
| Decisão de qualidade | sim | criado nesta entrega | [docs/decisao-qualidade.md](../decisao-qualidade.md) |

## Bloqueios encontrados (em ordem)

Nenhum passo do README falhou no clone limpo. Os bloqueios eram de documentação:

1. Não havia resultado de teste de carga nem registro de defeitos.
2. RTM com RF-05, RF-13 e RF-14 ainda marcados "a criar", e RF-05 sem teste para professor e material.
3. Faltava o diagrama de sequência crítica.
4. RTM e relatório de qualidade fora dos caminhos pedidos (`docs/RTM.md`, `docs/testes/relatorio-qualidade.md`).
5. O CI não rodava em tags, então não havia run ligado à release candidate.

Todos foram resolvidos na PR da `v1.0.0-rc1`, exceto a carga JMeter de 50 usuários. Ela ficou como risco aceito na [decisão de qualidade](../decisao-qualidade.md).
