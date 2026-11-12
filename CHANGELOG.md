# Changelog

## [v1.0.0-rc2] — 12/11/2026

### Correção

- **D-09 (RF-06):** com 50 pedidos simultâneos pela mesma sala, o pool de conexões esgotava e os perdedores recebiam timeout em vez de `409`. O conflito evitado agora é gravado depois que a transação da reserva termina. Teste de regressão com 20 pedidos simultâneos.

### Evidência nova

- JMeter com 50 usuários disputando a mesma sala, 3 rodadas: 1 `201`, 49 `409` e 0 erro técnico em cada, com persistência conferida no banco ([resultado-02](docs/performance/resultado-02.md)). O p95 ficou entre 1,1 s e 1,5 s (risco aceito R1).
- 225 testes, 0 pulados.

## [v1.0.0-rc1] — 12/11/2026

Primeira release candidate do sistema de organização de salas, professores e materiais.

### Funcionalidades

- **RF-01** Login HTTP Basic com perfis Solicitante, Responsável e Administrador; acesso negado por padrão.
- **RF-02** Cadastro e consulta de salas, professores e materiais.
- **RF-03** Pesquisa por texto, tipo e capacidade mínima.
- **RF-04** Criar, editar e cancelar reservas conforme perfil, titularidade e estado.
- **RF-05** Sala, professor e material não se sobrepõem no mesmo horário (restrição no banco).
- **RF-06** Duas reservas simultâneas: só uma é aceita.
- **RF-07** Recurso restrito exige aprovação ou rejeição do Responsável.
- **RF-08** Sala ou material em manutenção não entra em reserva nova.
- **RF-09** Retirada e devolução de recursos.
- **RF-10** Auditoria imutável de toda mudança, na mesma transação.
- **RF-11** Notificação externa depois do commit, com timeout de 2 s.
- **RF-12** Relatório de utilização, carga horária e conflitos evitados.
- **RF-13** Mensagens de erro neutras (400/401/403/404/409).
- **RF-14** README e contratos permitem reproduzir tudo a partir de um clone limpo.

### Qualidade

- 224 testes, 0 pulados; integração com PostgreSQL real (Testcontainers).
- Cobertura JaCoCo: 98,5% de linhas e 91,7% de branches.
- Quality Gate do SonarCloud aprovado.
- CI também roda em tags `v*`, para ligar a evidência à release candidate.

### Correções

Ver o [registro de defeitos](docs/defeitos/README.md).

### Limitações conhecidas

Ver os riscos aceitos na [decisão de qualidade](docs/decisao-qualidade.md).
