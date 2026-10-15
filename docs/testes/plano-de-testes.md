# Plano de testes inicial

## Objetivo

Reduzir os riscos críticos de autorização, sobreposição, dupla reserva, manutenção, transições inválidas e perda de auditoria.

## Níveis

- Unidade: invariantes, horários, estados e decisões de autorização.
- API/caixa-preta: contrato HTTP, validações, erros e perfis.
- Integração: PostgreSQL/Testcontainers, constraints e transações.
- Integração externa: WireMock para sucesso, timeout e indisponibilidade.
- End-to-end: fluxos principais dos três perfis.
- Performance: JMeter para carga nominal e disputa simultânea.

## Gates

- todos os testes verdes em `clean verify`;
- >= 80% de linhas e >= 70% de branches;
- zero falha crítica aberta;
- requisitos críticos com evidência no commit entregue.

