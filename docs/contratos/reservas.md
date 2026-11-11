# API de reservas

A API usa HTTP Basic. O Solicitante cria, edita e cancela as próprias reservas.
O Administrador pode editar e cancelar qualquer reserva, respeitando os mesmos estados.
A auditoria registra o usuário autenticado como autor de cada operação.

- `POST /api/v1/reservas` cria uma reserva e retorna `201`. Se todos os
  recursos forem comuns, inicia em `APROVADA`; se qualquer recurso for restrito,
  inicia em `SOLICITADA` e exige aprovação de um Responsável.
- `PUT /api/v1/reservas/{id}` substitui o período e os recursos e retorna `200`.
- `DELETE /api/v1/reservas/{id}` cancela a reserva e retorna `204`.
- `POST /api/v1/reservas/{id}/aprovar` aprova uma reserva `SOLICITADA` e retorna
  `200` com `id` e `estado`. Somente o perfil `RESPONSAVEL` pode chamar essa rota.

Na criação e na edição, o corpo contém `salaId`, `inicio` e `fim`, mais
`professorId` e `materialIds` opcionais. Os horários são instantes ISO 8601.
A criação e a edição retornam `id` e `estado`.

Edição e cancelamento aceitam `SOLICITADA` e `APROVADA`. A edição volta
para `SOLICITADA` se a composição anterior ou a nova tiver um recurso
restrito. O campo `restrito` da V7 é falso por padrão e a API de
catálogo ainda não expõe sua configuração. Cancelar muda o estado para
`CANCELADA` e libera as alocações na mesma transação da auditoria.

A API retorna `409` por sobreposição ou por estado que impede a operação.
Reserva inexistente retorna `404`. Solicitante que tenta alterar ou cancelar
uma reserva de outra pessoa recebe `403`.

Na reserva restrita, somente o Responsável pode realizar a transição
`SOLICITADA` → `APROVADA`. Solicitante e Administrador recebem `403`; tentativa
de aprovar reserva fora de `SOLICITADA` recebe `409`. A mudança e o evento
`RESERVA_APROVADA` são gravados na mesma transação, com o Responsável autenticado
como autor. Reservas que contêm apenas recursos comuns já nascem `APROVADA`,
pois não dependem de aprovação.

## Retirada e devolução (RF-09)

Somente Responsável ou Administrador registra; Solicitante recebe `403`.

- `POST /api/v1/reservas/{id}/retirada`: `APROVADA` → `EM_USO`. Retorna `200` com `id` e `estado`.
- `POST /api/v1/reservas/{id}/devolucao`: `EM_USO` → `CONCLUIDA`. Libera as alocações (o horário volta a ficar livre) e retorna `200`.

Estado diferente do exigido, inclusive repetir a mesma operação, retorna `409` sem alterar a reserva nem gerar evento. Reserva inexistente retorna `404`. A reserva é travada durante a operação: duas retiradas simultâneas resultam em uma aceita e uma `409`. Cada registro gera um evento de auditoria (`RETIRADA_REGISTRADA` ou `DEVOLUCAO_REGISTRADA`) com o usuário autenticado como autor, na mesma transação.

Fora do escopo: regra de horário (retirar antes do início ou devolver após o fim é aceito) e controle de quantidade de material (ver ADR 0005).
