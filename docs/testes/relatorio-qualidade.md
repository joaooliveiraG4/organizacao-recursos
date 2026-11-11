# Relatório de qualidade — release candidate

Data: 11/11/2026. Base medida: branch `feat/joao-relatorio-rf12` (contém a `main` com as PRs até a #10, mais as PRs #14 e #15). Comando: `.\mvnw.cmd clean verify` com Docker Desktop (PostgreSQL 17 real via Testcontainers). Os links de CI de cada requisito estão na [RTM](../RTM.md).

## Resultado geral

| Indicador | Meta da disciplina | Medido | Situação |
|---|---|---|---|
| Testes executados | — | **194**, 0 falhas, 0 erros, **0 pulados** | ✅ |
| Cobertura de linhas (JaCoCo) | ≥ 80% | **98,2%** (596/607) | ✅ |
| Cobertura de branches (JaCoCo) | ≥ 70% | **89,0%** (73/82) | ✅ |
| Requisitos críticos rastreados | 100% | RF-05, RF-06 e RF-07 na RTM; RF-07 ver pendências | ⚠️ |
| Bugs/vulnerabilidades críticas | zero | SonarCloud: Quality Gate aprovado, 0 issues novas nas PRs | ✅ (ver limitação do Sonar) |

## Estratégia de testes aplicada

| Tipo | Onde | Quantidade |
|---|---|---|
| Unitário | regras de domínio (`PeriodoReserva`, transições de `Reserva`, serviços do catálogo) | maioria das 27 classes de teste |
| Parametrizado (JUnit 5) | matriz de perfis do RF-01, estados do RF-09, filtros, códigos HTTP do provedor | 12 classes |
| Integração com PostgreSQL real (Testcontainers) | migrations, auditoria imutável, reservas, utilização, relatório, notificação | 11 classes |
| Integração externa simulada (WireMock) | RF-11: sucesso, timeout, 3xx/4xx/5xx, provedor fora do ar | 2 classes |
| API caixa-preta (MockMvc + HTTP Basic real) | reservas, retirada/devolução, relatório, catálogo | — |
| Concorrência | RF-06 (duas criações → 201 + 409) e RF-09 U07 (duas retiradas → 200 + 409) | 3 testes |
| Desempenho/disputa (JMeter) | plano e avaliador na PR #9 | ver pendências |

**Gate contra falso verde:** desde a PR #7 os testes de integração não usam `disabledWithoutDocker`. Sem Docker, a suíte reprova em vez de pular (comprovado em 11/11, quando o Docker estava desligado).

## Evidências de que os testes detectam defeitos (mutação manual)

Cada teste abaixo foi rodado com o código propositalmente quebrado e reprovou; depois o código foi restaurado.

| Requisito | Defeito introduzido | Resultado |
|---|---|---|
| RF-01 | catálogo aberto a qualquer perfil | 6 casos da matriz de perfis reprovaram |
| RF-03 | `capacidadeMinima` sem validação (teste escrito antes: TDD) | reprovou com 200, passou com 400 após a correção |
| RF-09 | remover a trava `PESSIMISTIC_WRITE` | duas retiradas simultâneas aceitas `[200, 200]` |
| RF-11 | remover `@Async` | quem aprova esperou 3,1 s (meta 2 s) |
| RF-11 | 302 tratado como entrega (achado na revisão do Kauã) | 301/302 reprovaram antes da correção |
| RF-12 | remover filtro de estado do relatório | reserva cancelada contada (3 em vez de 2) |

## Decisões de arquitetura registradas

ADR 0001 (stack e PostgreSQL), 0002 (autenticação e perfis), 0003 (auditoria transacional e imutável), 0004 (SonarCloud pelo CI), 0005 (material reservado inteiro). Modelo C4 e utility tree em `docs/arquitetura`.

## Pendências e limitações conhecidas

- **RF-07 (aprovação de recurso restrito):** responsabilidade do Kauã; enquanto não entrar, reservas permanecem `SOLICITADA` e a aprovação/notificação não acontecem pelo fluxo real.
- **RF-08 (manutenção):** responsabilidade da Miranda.
- **SonarCloud:** o repositório foi transferido para a conta pessoal em 10/11; a análise com cobertura do JaCoCo depende do `SONAR_TOKEN` no projeto novo. Até lá, o Sonar mostra 0,0% de cobertura em código novo (a cobertura real é a medida acima).
- **JMeter:** a ferramenta de disputa (PR #9) está pronta; a execução contra a API rodando fica registrada quando a #9 entrar.
- **Fora do escopo por decisão:** reenvio de notificação (outbox), quantidade de material (ADR 0005), regra de horário para retirada/devolução, horas do relatório sem recorte nas bordas do período.
