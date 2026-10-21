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
- análise estática: [painel do projeto no SonarCloud](https://sonarcloud.io/project/overview?id=joaooliveiraG4_organizacao-recursos), executada automaticamente em pushes e pull requests.

## Documentação

- [Plano de recuperação](docs/plano-recuperacao-semana-42.md)
- [Matriz de rastreabilidade](docs/RTM.md)
- [Plano de testes](docs/testes/plano-de-testes.md)
- [Contrato da API de salas](docs/contratos/salas.md)
- [Arquitetura](docs/arquitetura/README.md)
- [Prompts da equipe](docs/prompts-da-equipe.md)

Material oficial da disciplina: https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html
