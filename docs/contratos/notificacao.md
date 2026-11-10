# Contrato — notificação externa (RF-11)

Quando uma reserva é aprovada, rejeitada ou cancelada, o sistema avisa o solicitante por um serviço externo de notificação. Nos testes, esse serviço é simulado com WireMock.

## Chamada ao provedor

```http
POST {NOTIFICACAO_URL}
Content-Type: application/json

{
  "destinatarioId": "11111111-1111-1111-1111-111111111111",
  "tipo": "RESERVA_APROVADA",
  "referenciaId": "22222222-2222-2222-2222-222222222222"
}
```

- `tipo`: `RESERVA_APROVADA`, `RESERVA_REJEITADA` ou `RESERVA_CANCELADA`.
- Somente ids (LGPD): nome, e-mail ou telefone não saem do sistema. O provedor busca o contato por conta própria.
- Qualquer resposta 2xx significa entregue.

## Configuração

| Variável | Padrão | Uso |
|---|---|---|
| `NOTIFICACAO_URL` | `http://localhost:8089/notificacoes` | endereço do provedor |
| `NOTIFICACAO_TIMEOUT` | `2s` | espera máxima por conexão e por resposta |

## Comportamento (decisões do João em 09/11/2026)

1. **Só depois do commit.** O aviso é publicado dentro da transação da mudança de estado e enviado apenas quando ela é confirmada (ADR 0003). Se a transação for desfeita, nada é enviado.
2. **Em segundo plano.** O envio roda em outra thread: quem aprova recebe a resposta sem esperar o provedor.
3. **Falha registra e segue.** Timeout, erro 4xx/5xx ou provedor fora do ar viram um resultado (`TIMEOUT`, `ERRO_DO_PROVEDOR`, `INDISPONIVEL`) registrado no log com tipo e id da reserva. A mudança de estado continua valendo e não há nova tentativa.

## Como usar em um fluxo de reserva

```java
@Transactional
public void aprovar(UUID reservaId, UsuarioAutenticado usuario) {
    // ... muda o estado e registra a auditoria (RF-10)
    eventos.publishEvent(new Notificacao(
            reserva.getSolicitanteId(), TipoNotificacao.RESERVA_APROVADA, reserva.getId()));
}
```

`eventos` é o `ApplicationEventPublisher` do Spring. Publicar fora de uma transação não envia nada; a publicação precisa estar dentro do método `@Transactional`.

## Limitações conhecidas

- Sem nova tentativa: um aviso perdido não é reenviado. Melhoria futura: tabela de saída (*outbox*) com reenvio.
- O resultado fica só no log, não em tabela.
