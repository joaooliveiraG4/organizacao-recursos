# Fluxo Git e pull request

## Antes da branch

1. Confirmar identidade e tarefa escolhida.
2. Executar `git status --short` e não sobrescrever trabalho existente.
3. Confirmar que `main` está limpa e sincronizada quando houver acesso ao remote.
4. Criar branch exclusiva:
   - código: `feat/<nome>-<resultado>`;
   - correção: `fix/<nome>-<resultado>`;
   - documentação: `docs/<nome>-<resultado>`;
   - testes/qualidade: `test/<nome>-<resultado>`.

Use `joao`, `miranda` ou `kaua`, sem acento, no nome da branch.

## Conteúdo da contribuição

- código e configuração necessários;
- testes proporcionais ao risco;
- atualização da RTM;
- documentação ou ADR quando houver decisão relevante;
- nenhuma alteração oportunista fora do escopo.

## Verificação mínima

Use o Maven Wrapper do projeto. A verificação final esperada é `./mvnw clean verify` (ou `mvnw.cmd clean verify` no Windows). Execute também verificações específicas de integração, cobertura ou carga quando forem parte do card.

## Pull request

O título deve descrever o resultado. O corpo precisa conter:

- responsável;
- problema e requisito;
- solução;
- testes executados e resultado;
- evidências/links;
- impacto na RTM;
- limitações e riscos residuais;
- roteiro curto para revisão manual.

A PR é aberta para revisão; o agente nunca faz merge automaticamente.

