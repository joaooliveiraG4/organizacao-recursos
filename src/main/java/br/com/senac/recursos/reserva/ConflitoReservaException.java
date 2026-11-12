package br.com.senac.recursos.reserva;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 409 por sobreposição (RF-05/06). Carrega o recurso e o período para que o
 * conflito evitado (RF-12) seja gravado depois que a transação da reserva for
 * desfeita e tiver devolvido a conexão ao pool (defeito D-09).
 */
public class ConflitoReservaException extends ResponseStatusException {

    private final String tipoRecurso;
    private final UUID recursoId;
    private final transient PeriodoReserva periodo;

    public ConflitoReservaException(String tipoRecurso, UUID recursoId, PeriodoReserva periodo) {
        super(HttpStatus.CONFLICT, "Recurso indisponível no período.");
        this.tipoRecurso = tipoRecurso;
        this.recursoId = recursoId;
        this.periodo = periodo;
    }

    public String tipoRecurso() {
        return tipoRecurso;
    }

    public UUID recursoId() {
        return recursoId;
    }

    public PeriodoReserva periodo() {
        return periodo;
    }
}
