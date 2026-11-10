# API de reservas

A API usa HTTP Basic. O Solicitante cria, edita e cancela as próprias reservas.
O Administrador pode editar e cancelar qualquer reserva, respeitando os mesmos estados.
A auditoria registra o usuário autenticado como autor de cada operação.

- `POST /api/v1/reservas` cria uma reserva `SOLICITADA` e retorna `201`.
- `PUT /api/v1/reservas/{id}` substitui o período e os recursos e retorna `200`.
- `DELETE /api/v1/reservas/{id}` cancela a reserva e retorna `204`.

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
