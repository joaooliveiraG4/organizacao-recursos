# API de reservas

O perfil Solicitante usa HTTP Basic e opera apenas as próprias reservas.

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
Reservas inexistentes ou de outro solicitante retornam `404`.
