# Equipe e responsabilidades iniciais

Esta distribuição organiza o trabalho atrasado e pode ser alterada por decisão explícita da equipe. Ela não reduz a responsabilidade coletiva: todos devem compreender os fluxos críticos e saber defender a própria contribuição.

## João — integração e qualidade

Responsabilidades iniciais:

1. Estrutura do projeto Java 21 e Spring Boot 3.x.
2. Modelo de domínio compartilhado, autenticação/autorização e auditoria.
3. GitHub Actions, JaCoCo e SonarCloud.
4. Arquitetura C4, utility tree, ADRs e RTM.
5. Integração final, release candidate e relatório de qualidade.

Forma de orientação: discutir arquitetura e trade-offs, mas manter explicações claras e registrar decisões. João revisa as PRs e coordena dependências; não deve absorver silenciosamente toda a autoria do grupo.

## Miranda — catálogo e administração de recursos

Responsabilidades iniciais:

1. Cadastro e consulta de salas, professores e materiais.
2. Pesquisa por tipo, capacidade, localização e competência.
3. Bloqueio e manutenção de recursos.
4. Testes unitários, de API e integração desses fluxos.
5. Documentação do contrato e atualização da RTM.

Forma de orientação: passos pequenos, exemplos concretos e separação clara entre entrada da API, regra de negócio, persistência e resposta. Sempre explicar como validações e estados impedem dados inválidos.

## Kauã — reservas e utilização

Responsabilidades iniciais:

1. Criar, alterar e cancelar reservas.
2. Detectar sobreposição de sala, material e professor.
3. Aprovação de recursos restritos e transições de estado.
4. Retirada e devolução de materiais.
5. Testes de concorrência e cenário JMeter de dupla reserva.

Forma de orientação: uma decisão de negócio por vez, diagramas ou exemplos simples e validação frequente. Reforçar que conflito esperado é comportamento correto, enquanto dupla reserva aceita é defeito crítico.

## Conhecimento comum obrigatório

Os três integrantes devem conseguir explicar:

- por que Foot Fanatics não é o produto entregue;
- os perfis Solicitante, Responsável e Administrador;
- o risco crítico de dupla reserva;
- a diferença entre cobertura e qualidade;
- como a RTM liga requisito, risco, teste e evidência;
- o que o pipeline verifica;
- quais riscos residuais permanecem na versão entregue.

