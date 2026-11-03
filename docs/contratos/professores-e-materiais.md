# Contrato da API de professores e materiais

Todos os endpoints exigem autenticação conforme a configuração atual da API.

## Professores

### Cadastrar professor

`POST /api/v1/professores`

```json
{
  "codigo": "PROF-101",
  "nome": "Maria Silva",
  "competencia": "Matemática"
}
```

- `codigo`: obrigatório, até 30 caracteres e único sem diferenciar maiúsculas/minúsculas;
- `nome`: obrigatório, até 120 caracteres;
- `competencia`: obrigatória, até 120 caracteres.

Retorna `201 Created`, o professor e o cabeçalho `Location` para consulta. Código repetido retorna `409 Conflict`; dados inválidos retornam `400 Bad Request`.

### Consultar professores

- `GET /api/v1/professores`: lista professores em ordem de código;
- `GET /api/v1/professores/{id}`: consulta pelo UUID; retorna `404 Not Found` se não existir.

## Materiais

### Cadastrar material

`POST /api/v1/materiais`

```json
{
  "codigo": "MAT-201",
  "nome": "Projetor",
  "tipo": "audiovisual",
  "quantidade": 2
}
```

- `codigo`: obrigatório, até 30 caracteres e único sem diferenciar maiúsculas/minúsculas;
- `nome`: obrigatório, até 120 caracteres;
- `tipo`: obrigatório, até 60 caracteres;
- `quantidade`: número inteiro maior que zero.

Retorna `201 Created`, o material e o cabeçalho `Location` para consulta. Código repetido retorna `409 Conflict`; dados inválidos retornam `400 Bad Request`.

### Consultar materiais

- `GET /api/v1/materiais`: lista materiais em ordem de código;
- `GET /api/v1/materiais/{id}`: consulta pelo UUID; retorna `404 Not Found` se não existir.

Os códigos são removidos de espaços externos e normalizados em maiúsculas antes da gravação; a unicidade também é imposta no PostgreSQL. A quantidade representa o total cadastrado nesta fatia, não a disponibilidade para empréstimo. Retirada/devolução pertence ao RF-09. Pesquisa por competência, tipo ou disponibilidade pertence ao RF-03.

## Dependência de migration

A migration `V3__professores_e_materiais.sql` pressupõe que a migration `V2__salas.sql` da PR #2 seja aplicada antes. Para manter a ordem do Flyway, a PR #2 deve ser mesclada antes desta contribuição.
