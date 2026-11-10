# Contrato de autenticação e perfis

Decisão completa: [ADR 0002](../adr/0002-autenticacao-e-perfis.md).

## Como autenticar

Envie o cabeçalho HTTP Basic com e-mail e senha em todas as requisições, exceto `GET /api/v1/status` e `GET /actuator/health`.

```powershell
curl.exe -u admin@exemplo.test:<senha> http://localhost:8080/api/v1/me
```

## Usuário atual

`GET /api/v1/me` retorna `200 OK`:

```json
{
  "id": "7f1c...",
  "nome": "Administrador inicial",
  "email": "admin@exemplo.test",
  "perfil": "ADMINISTRADOR"
}
```

## Permissões atuais

| Operação | Solicitante | Responsável | Administrador |
|---|---|---|---|
| `GET /api/v1/me` | sim | sim | sim |
| `GET` em salas, professores e materiais | sim | sim | sim |
| `POST`/`PUT`/`PATCH`/`DELETE` em salas, professores e materiais | não (403) | não (403) | sim |

## Erros

| Situação | Status | `detail` |
|---|---|---|
| Sem credencial, senha errada, e-mail inexistente ou usuário inativo | `401` | Credenciais ausentes ou inválidas. |
| Autenticado, mas o perfil não permite a operação | `403` | Seu perfil não tem permissão para esta operação. |

As respostas usam `application/problem+json`.
