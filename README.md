# Organização de Recursos

Projeto acadêmico de Qualidade de Software para alocação de salas, professores e materiais sem conflitos. O foco é produzir uma aplicação executável acompanhada de evidências reproduzíveis de qualidade, segurança, rastreabilidade e desempenho.

## Requisitos locais

- Git;
- Java 21;
- Docker Desktop para PostgreSQL e Testcontainers;
- Codex aberto na raiz do repositório para o fluxo orientado por agente.

Não é necessário instalar Maven: o repositório utiliza Maven Wrapper.

## Primeiro uso

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Verifique `http://localhost:8080/api/v1/status` e `http://localhost:8080/actuator/health`.

### Administrador local

Os demais endpoints exigem login. Para criar um Administrador na primeira execução, defina as variáveis no terminal antes de subir a aplicação (use valores próprios e não os versione):

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@exemplo.test"
$env:BOOTSTRAP_ADMIN_PASSWORD = "<escolha-uma-senha>"
.\mvnw.cmd spring-boot:run
curl.exe -u admin@exemplo.test:<senha> http://localhost:8080/api/v1/me
```

Perfis, permissões e respostas de erro: [contrato de autenticação](docs/contratos/autenticacao.md).

### Banco local com migrations fora de ordem

Se a aplicação não subir com erro do Flyway como `Detected resolved migration not applied to database`, o banco local recebeu uma migration de número maior antes de uma menor (por exemplo, rodou a V3 antes de a V2 entrar na `main`). Em ambiente local, recrie o banco; os dados são apagados:

```powershell
docker compose down -v
docker compose up -d
```

## Fluxo da equipe

1. Clone o repositório e abra sua raiz no VS Code/Codex.
2. Leia ou envie o prompt de `docs/prompts-da-equipe.md` correspondente ao seu nome.
3. Escolha uma das tarefas apresentadas pelo agente.
4. Compreenda requisito, risco e teste antes da implementação.
5. Trabalhe em branch própria e abra uma pull request.
6. Não faça merge da própria PR.

O `AGENTS.md` carrega o fluxo pedagógico e operacional diretamente do repositório. Nenhuma instalação manual de skill é necessária.

## Verificação

```powershell
.\mvnw.cmd clean verify
```

Relatórios:

- testes: `target/surefire-reports` e `target/failsafe-reports`;
- cobertura: `target/site/jacoco/index.html`.
- análise estática: [painel do projeto no SonarCloud](https://sonarcloud.io/project/overview?id=joaovictordasilvaolveira_organizacao-recursos), executada pelo GitHub Actions após `clean verify`, com a cobertura do JaCoCo ([ADR 0004](docs/adr/0004-sonarcloud-pelo-ci.md)).

## Documentação

- [Plano de recuperação](docs/plano-recuperacao-semana-42.md)
- [Matriz de rastreabilidade](docs/rtm.md)
- [Plano de testes](docs/testes/plano-de-testes.md)
- [Contrato da API de salas](docs/contratos/salas.md)
- [Arquitetura](docs/arquitetura/README.md)
- [Relatório de qualidade](docs/relatorio-qualidade.md)
- [Decisão de qualidade (v1.0.0-rc1)](docs/decisao-qualidade.md)
- [Índice de evidências](docs/evidencias/README.md)
- [Checklist de entrega](docs/entrega/checklist.md)
- [Registro de defeitos](docs/defeitos/README.md)
- [Resultado da disputa real (RF-06)](docs/performance/resultado-01.md)
- [Resultado do JMeter com 50 usuários (RF-06)](docs/performance/resultado-02.md)
- [Sequência crítica: criar reserva](docs/arquitetura/sequencia-reserva.md)
- [Changelog](CHANGELOG.md)
- [ADR 0002 — autenticação e perfis](docs/adr/0002-autenticacao-e-perfis.md)
- [ADR 0003 — auditoria transacional e imutável](docs/adr/0003-auditoria-transacional-e-imutavel.md)
- [Prompts da equipe](docs/prompts-da-equipe.md)
- [Contrato da API de professores e materiais](docs/contratos/professores-e-materiais.md)
- [Contrato da notificação externa (RF-11)](docs/contratos/notificacao.md)
- [Contrato da API de reservas](docs/contratos/reservas.md)
- [Contrato do relatório de utilização (RF-12)](docs/contratos/relatorios.md)

Material oficial da disciplina: https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html
