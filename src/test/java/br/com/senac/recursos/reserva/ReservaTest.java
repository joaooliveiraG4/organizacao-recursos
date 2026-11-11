package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReservaTest {

    @Test
    void novaReservaMantemSolicitanteEPeriodoNoEstadoInicial() {
        var solicitante = UUID.randomUUID();
        var periodo = new PeriodoReserva(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600));

        var reserva = new Reserva(solicitante, periodo);

        assertThat(reserva.getId()).isNotNull();
        assertThat(reserva.getSolicitanteId()).isEqualTo(solicitante);
        assertThat(reserva.getPeriodo()).isEqualTo(periodo);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }
}
