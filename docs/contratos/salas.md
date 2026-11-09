# Contrato da API de salas

Todos os endpoints exigem autenticação conforme a configuração atual da API.

## Cadastrar sala

`POST /api/v1/salas`

```json
{
  "codigo": "B-101",
  "tipo": "Aula",
  "capacidade": 30,
  "localizacao": "Bloco B"
}
```

- `codigo`: obrigatório, até 30 caracteres e único sem diferenciar maiúsculas/minúsculas;
- `tipo`: obrigatório, até 60 caracteres;
- `capacidade`: número inteiro maior que zero;
- `localizacao`: obrigatória, até 120 caracteres.

Retorna `201 Created`, o recurso cadastrado e o cabeçalho `Location` para consultá-lo. Dados inválidos retornam `400 Bad Request`; código já cadastrado retorna `409 Conflict`.

## Consultar salas

- `GET /api/v1/salas`: lista salas em ordem de código;
- `GET /api/v1/salas/{id}`: consulta uma sala pelo identificador UUID; retorna `404 Not Found` se não existir.

Cada resposta contém `id`, `codigo`, `tipo`, `capacidade` e `localizacao`. Os filtros por tipo, capacidade e localização pertencem ao requisito RF-03 e não fazem parte desta entrega.
