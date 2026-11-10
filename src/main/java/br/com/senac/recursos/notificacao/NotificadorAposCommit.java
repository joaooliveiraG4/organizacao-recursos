package br.com.senac.recursos.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RF-11 — envia a notificação somente depois que a transação da mudança de
 * estado foi confirmada (ADR 0003), e em outra thread: quem aprovou recebe a
 * resposta sem esperar o provedor. Se a transação for desfeita, nada é enviado.
 * Falha no envio é registrada e não desfaz nada (decisão do João, 09/11/2026).
 */
@Component
public class NotificadorAposCommit {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorAposCommit.class);

    private final ClienteNotificacao cliente;

    public NotificadorAposCommit(ClienteNotificacao cliente) {
        this.cliente = cliente;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmar(Notificacao notificacao) {
        var resultado = cliente.enviar(notificacao);
        if (resultado == ResultadoEnvio.ENVIADA) {
            LOG.info("Notificação enviada: tipo={} referencia={} resultado={}",
                    notificacao.tipo(), notificacao.referenciaId(), resultado);
        } else {
            LOG.warn("Notificação não entregue: tipo={} referencia={} resultado={}",
                    notificacao.tipo(), notificacao.referenciaId(), resultado);
        }
    }
}
