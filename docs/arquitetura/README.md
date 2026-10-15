# Arquitetura

Esta pasta receberá os diagramas C4, cenários ATAM, utility tree, riscos e trade-offs. Decisões arquiteturais ficam em `docs/adr`.

## Direção inicial

- aplicação modular Spring Boot;
- API versionada em `/api/v1`;
- PostgreSQL com migrations Flyway;
- regras críticas no domínio/serviço, não somente no controller;
- tempo injetado por `Clock`;
- autorização negada por padrão;
- auditoria de transições de estado;
- concorrência protegida no banco e validada por teste realista.

