# Arquitetura

Decisões arquiteturais ficam em `docs/adr`.

- [Modelo C4](c4.md): contexto, containers e componentes da API, com a situação de cada parte.
- [Utility tree](utility-tree.md): cenários de qualidade mensuráveis, riscos, pontos de sensibilidade e trade-offs.

## Direção inicial

- aplicação modular Spring Boot;
- API versionada em `/api/v1`;
- PostgreSQL com migrations Flyway;
- regras críticas no domínio/serviço, não somente no controller;
- tempo injetado por `Clock`;
- autorização negada por padrão;
- auditoria de transições de estado;
- concorrência protegida no banco e validada por teste realista.

