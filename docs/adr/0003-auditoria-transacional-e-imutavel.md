# ADR 0003 — Auditoria transacional e imutável

- Status: aceita
- Data: 2026-10-27
- Responsável: João
- Requisito: RF-10

## Contexto

A regra crítica do projeto é: toda mudança de estado gera auditoria. Sem histórico confiável não há como provar quem aprovou, cancelou ou marcou "não compareceu" em uma reserva. A tabela `audit_event` existia desde a V1, mas nada gravava nela e qualquer linha podia ser alterada ou apagada.

## Decisão

- `AuditoriaService.registrar(tipo, registroId, acao, autorId, detalhes)` é a única porta de escrita. Ela não depende de reservas nem de autenticação, para poder ser usada por qualquer módulo.
- **Atomicidade:** `registrar` usa `Propagation.MANDATORY`, então só funciona dentro da transação da própria mudança de estado. A mudança e o evento são confirmados juntos ou desfeitos juntos. Falha ao auditar significa que a operação não acontece; o usuário recebe erro e pode repetir.
- **Efeitos externos** (notificação, RF-11) ficam fora dessa transação e rodam depois da confirmação; uma falha neles não desfaz a mudança nem o evento.
- **Autor obrigatório em todos os casos** (decisão do João em 2026-10-27). `actor_id` é `NOT NULL`. Ações automáticas usam o autor técnico `AuditoriaService.AUTOR_SISTEMA` (`00000000-0000-0000-0000-000000000000`).
- **Imutabilidade no banco:** triggers da V4 recusam `UPDATE`, `DELETE` e `TRUNCATE` em `audit_event`, inclusive vindos de scripts ou acessos diretos. A entidade também é `@Immutable` no Hibernate.
- **Ordem:** o horário vem do `Clock` injetado; `sequence_number` desempata eventos do mesmo instante.
- **Minimização de dados (LGPD):** pessoas são identificadas apenas por id. Nome, e-mail ou telefone não entram em `detalhes`, porque um evento imutável não pode ser anonimizado depois. A exibição busca esses dados na tabela de usuários no momento da consulta.

## Exemplo de uso

```java
@Transactional
public void aprovar(UUID reservaId, UsuarioAutenticado usuario) {
    var reserva = ...;                       // muda o estado
    auditoria.registrar("RESERVA", reserva.getId(), "RESERVA_APROVADA", usuario.id(),
            Map.of("de", "SOLICITADA", "para", "APROVADA"));
}
// notificação: somente após o commit
```

## Consequências

- Testes não conseguem limpar `audit_event`; cada teste usa registros com ids novos.
- Correção de evento errado é feita com um novo evento compensatório, nunca editando o anterior.
- Quem tiver privilégio de dono da tabela no banco ainda pode desativar o trigger; isso fica fora do alcance da aplicação.

## Fora do escopo desta decisão

- Endpoint de consulta do histórico e quais eventos cada perfil enxerga: depende dos perfis do RF-01 (PR #3) e será feito em PR separada.
- Registrar as transições de reserva: responsabilidade do fluxo de reservas (Kauã), usando este serviço.
