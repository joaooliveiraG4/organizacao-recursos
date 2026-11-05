package br.com.senac.recursos.reserva;

import java.time.Instant;
import java.util.Objects;

/**
 * Período em UTC com início inclusivo e fim exclusivo: [início, fim).
 * Permite que a próxima reserva comece exatamente quando esta terminar.
 */
public record PeriodoReserva(Instant inicio, Instant fim) {

    public PeriodoReserva {
        Objects.requireNonNull(inicio, "Informe o início da reserva");
        Objects.requireNonNull(fim, "Informe o fim da reserva");
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O fim da reserva deve ser posterior ao início");
        }
    }

    /**
     * Compara somente os horários. O serviço de reservas deve selecionar
     * as reservas do mesmo recurso/professor antes de usar esta regra.
     */
    public boolean sobrepoe(PeriodoReserva outro) {
        Objects.requireNonNull(outro, "Informe o período a comparar");
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }
}