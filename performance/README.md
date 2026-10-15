# Testes de desempenho

Os planos JMeter ficarão em `performance/jmeter`. Resultados brutos locais não devem ser versionados; o relatório final deve registrar cenário, ambiente, commit, métricas e conclusão.

## Cenário crítico planejado

Duas ou mais solicitações simultâneas disputam o mesmo recurso e período. Exatamente uma reserva pode ser aceita; as demais devem receber conflito de negócio controlado, sem erro `500` e sem estado duplicado no banco.

