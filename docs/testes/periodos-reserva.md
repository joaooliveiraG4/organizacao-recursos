# Validação dos períodos de reserva — primeira etapa

Responsável: Kauã. Requisitos relacionados: RF-04 e RF-05.

## Regra confirmada

Uma reserva usa início inclusivo e fim exclusivo: `[início, fim)`.
Se já existe uma reserva das 14h às 16h, uma nova das 15h às 17h tem conflito.
Uma nova reserva pode começar exatamente às 16h.

O fim deve ser estritamente posterior ao início. Os horários são comparados
como instantes UTC, preservando a precisão recebida. O período não consulta
o relógio atual nem decide se é permitido reservar no passado.

## Implementação e limites

`PeriodoReserva` centraliza a validação da duração e a comparação temporal.
A regra é: o início de cada período deve ser anterior ao fim do outro para
haver sobreposição. A comparação produz o mesmo resultado nos dois sentidos.

Esta classe não reserva recursos nem acessa o banco. Na integração, o serviço
deverá selecionar reservas do mesmo recurso ou professor, considerar seus
estados e tratar concorrência no PostgreSQL. Somente comparar horários não
impede duas requisições simultâneas de gravarem reservas conflitantes.

Não foram criados cadastros paralelos de salas, materiais, professores ou usuários.
O CRUD de reservas e a prevenção persistente de conflitos continuam pendentes.

## Testes reproduzíveis

Com Java 21 configurado na sessão, execute na raiz:

```powershell
.\mvnw.cmd clean verify
```

Os casos de `PeriodoReservaTest` cobrem intervalos separados, consecutivos,
sobreposição parcial, contenção, igualdade, início/fim ausentes, duração
nula/negativa, comparação ausente, fusos equivalentes, virada de dia e
precisão inferior a um segundo. Relatórios: `target/surefire-reports` e
`target/site/jacoco/index.html`. A execução de CI ficará ligada ao commit da PR.

## Decisões necessárias para o CRUD

- Quais estados permitem alterar ou cancelar uma reserva já iniciada?
- Reservas no passado são permitidas?
- Quais contratos dos cadastros e da autenticação serão utilizados pelo serviço?
- Quais estados devem bloquear a agenda?

Essas políticas não são definidas por esta classe. Não considerar RF-04 ou
RF-05 concluídos com esta primeira etapa.
## Resultado local — 27/10/2026

- Maven Wrapper com Java 21: `clean verify` aprovado.
- 20 testes executados, zero falhas, erros ou testes ignorados:
  19 de `PeriodoReservaTest` e 1 de `StatusControllerTest`.
- `PeriodoReserva`: 8/8 linhas e 6/6 condições cobertas no JaCoCo.
- Gate configurado no projeto aprovado, sem alterar exclusões ou limites.
- `git diff --check` aprovado.
- Nenhum warning Maven identificado no log desta execução.
- Failsafe informou que não há testes de integração. Nenhum teste de banco,
  API de reservas ou concorrência foi executado; esses fluxos ainda não existem
  nesta contribuição. Docker não está disponível nesta máquina.

O Java portátil e o cache Maven estão em `.m2-local/`, ignorada pelo Git.
A configuração global de Java não foi alterada. Os resultados acima descrevem
a execução local; a evidência de CI vinculada ao commit deve ser conferida na PR.