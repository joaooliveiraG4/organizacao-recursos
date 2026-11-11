package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void aprovarTransicionaDeSolicitadaParaAprovada() {
        var reserva = novaReserva();

        reserva.aprovar();

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.APROVADA);
    }

    @ParameterizedTest(name = "aprovação recusada em {0}")
    @EnumSource(value = EstadoReserva.class, names = "SOLICITADA", mode = EnumSource.Mode.EXCLUDE)
    void aprovarSoAcontecePartindoDeSolicitada(EstadoReserva estado) {
        var reserva = novaReserva();
        ReflectionTestUtils.setField(reserva, "estado", estado);

        assertThatThrownBy(reserva::aprovar).isInstanceOf(TransicaoInvalidaException.class);
        assertThat(reserva.getEstado()).isEqualTo(estado);
    }

    private static Reserva novaReserva() {
        return new Reserva(UUID.randomUUID(), new PeriodoReserva(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600)));
    }
}
