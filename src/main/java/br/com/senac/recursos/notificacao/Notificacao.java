package br.com.senac.recursos.notificacao;

import java.util.Objects;
import java.util.UUID;

/**
 * RF-11 — aviso enviado ao serviço externo. Leva apenas ids (LGPD): o provedor
 * busca nome e contato por conta própria, e nenhum dado pessoal sai daqui.
 *
 * <p>Para notificar, publique este objeto com {@code ApplicationEventPublisher}
 * dentro da transação da mudança de estado. Ele só é enviado depois do commit.
 */
public record Notificacao(UUID destinatarioId, TipoNotificacao tipo, UUID referenciaId) {

    public Notificacao {
        Objects.requireNonNull(destinatarioId, "destinatarioId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(referenciaId, "referenciaId");
    }
}
