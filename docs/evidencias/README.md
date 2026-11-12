# Evidências da release candidate `v1.0.0-rc1`

Tudo aqui se refere ao **commit da tag `v1.0.0-rc1`**. Para conferir o commit:

```bash
git rev-parse v1.0.0-rc1^{commit}
```

| Evidência | Onde | O que mostra |
|---|---|---|
| CI do commit da tag | [runs do workflow `qualidade` para a tag `v1.0.0-rc1`](https://github.com/joaovictordasilvaolveira/organizacao-recursos/actions/workflows/quality.yml?query=branch%3Av1.0.0-rc1) | `clean verify` com PostgreSQL real (Testcontainers), 0 testes pulados, gates do JaCoCo. O push na `main` do mesmo commit também tem run verde, que inclui o SonarCloud |
| Relatório JaCoCo | artefato **`relatorios-N`** do run acima (seção *Artifacts*), pasta `target/site/jacoco/` | cobertura de linhas e branches; o mesmo artefato traz `surefire-reports` e `failsafe-reports` |
| Quality Gate do SonarCloud | [painel do projeto](https://sonarcloud.io/project/overview?id=joaovictordasilvaolveira_organizacao-recursos) | Quality Gate aprovado na `main`; S2077 corrigido no código; S4502 aceito ([ADR 0006](../adr/0006-csrf-em-api-stateless.md)) |
| Resultado de desempenho/disputa | [docs/performance/resultado-01.md](../performance/resultado-01.md) | 20 pedidos simultâneos pela mesma sala: 1 `201`, 19 `409`, 0 erro técnico, 1 alocação no banco |
| Rastreabilidade | [docs/rtm.md](../rtm.md) | RF-01 a RF-14 e RNFs ligados a risco, teste e evidência |
| Defeitos | [docs/defeitos/](../defeitos/README.md) | 8 defeitos reais com causa, correção e teste de regressão |
| Relatório de qualidade | [docs/relatorio-qualidade.md](../relatorio-qualidade.md) | testes, cobertura, mutação manual, pendências |
| Checklist do clone limpo | [docs/entrega/checklist.md](../entrega/checklist.md) | README reproduzido do zero; itens com caminho |
| Decisão | [docs/decisao-qualidade.md](../decisao-qualidade.md) | liberar com ressalvas, critérios e riscos aceitos |

## Regra das tags

Uma tag já usada nunca é movida. Se precisar corrigir algo depois da `v1.0.0-rc1`, a correção entra por PR e vira `v1.0.0-rc2` no novo commit.
