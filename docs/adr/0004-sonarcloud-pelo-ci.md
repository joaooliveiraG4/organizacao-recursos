# ADR 0004 — Análise do SonarCloud pelo CI, com cobertura

- Status: aceita, ativação pendente de configuração
- Data: 2026-11-03
- Responsável: João
- Requisito: RNF-QUAL-01

## Contexto

A análise automática do SonarCloud não executa o build e não lê o relatório do JaCoCo. Nas PRs #3 e #6 o Quality Gate passou com "0.0% coverage on new code": o gate aprovava sem medir cobertura.

## Decisão

- A análise passa a rodar no GitHub Actions, depois de `mvn clean verify`, com `mvn sonar:sonar -Dsonar.qualitygate.wait=true`, enviando `target/site/jacoco/jacoco.xml`. O passo espera o resultado do Quality Gate: gate reprovado deixa o CI vermelho.
- A configuração fica no `pom.xml` (o scanner Maven não lê `sonar-project.properties`, que foi removido para não haver duas fontes).
- `sonar.coverage.exclusions` repete as exclusões do gate do JaCoCo (classe `main` e `config/**`).
- O passo só roda quando o segredo `SONAR_TOKEN` existe; sem ele, o CI continua verde e só o JaCoCo vale.

## Ativação (João, no navegador)

1. SonarCloud → projeto → **Administration → Analysis Method** → desligar **Automatic Analysis**. Com as duas ligadas, a análise pelo CI falha.
2. SonarCloud → **My Account → Security** → gerar um token.
3. GitHub → **Settings → Secrets and variables → Actions** → criar o segredo `SONAR_TOKEN` com esse token.
4. Rodar o CI de novo em uma PR e conferir que o Quality Gate mostra cobertura diferente de 0%.

## Consequências

- A cobertura passa a fazer parte do Quality Gate do SonarCloud, além do gate do JaCoCo.
- PRs vindas de forks não recebem o segredo e ficam sem análise do Sonar; a equipe trabalha com branches no próprio repositório.
- Se o token expirar, o passo falha e o CI fica vermelho, o que torna o problema visível.
