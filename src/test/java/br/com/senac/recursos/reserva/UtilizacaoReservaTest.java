package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RF-09 — transições de retirada e devolução na entidade. Fluxo principal:
 * APROVADA → EM_USO (retirada) → CONCLUIDA (devolução). Qualquer outro estado
 * de origem é recusado sem alterar a reserva.
 */
class UtilizacaoReservaTest {

    private static Reserva reservaEm(EstadoReserva estado) {
        var reserva = new Reserva(UUID.randomUUID(),
                new PeriodoReserva(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600)));
        ReflectionTestUtils.setField(reserva, "estado", estado);
        return reserva;
    }

    @Test
    void retiradaLevaReservaAprovadaParaEmUso() {
        var reserva = reservaEm(EstadoReserva.APROVADA);

        reserva.registrarRetirada();

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EM_USO);
    }

    @Test
    void devolucaoLevaReservaEmUsoParaConcluida() {
        var reserva = reservaEm(EstadoReserva.EM_USO);

        reserva.registrarDevolucao();

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONCLUIDA);
    }

    @ParameterizedTest(name = "retirada recusada em {0}")
    @EnumSource(value = EstadoReserva.class, names = "APROVADA", mode = EnumSource.Mode.EXCLUDE)
    void retiradaSoAcontecePartindoDeAprovada(EstadoReserva estado) {
        var reserva = reservaEm(estado);

        assertThatThrownBy(reserva::registrarRetirada).isInstanceOf(TransicaoInvalidaException.class);
        assertThat(reserva.getEstado()).isEqualTo(estado);
    }

    @ParameterizedTest(name = "devolução recusada em {0}")
    @EnumSource(value = EstadoReserva.class, names = "EM_USO", mode = EnumSource.Mode.EXCLUDE)
    void devolucaoSoAcontecePartindoDeEmUso(EstadoReserva estado) {
        var reserva = reservaEm(estado);

        assertThatThrownBy(reserva::registrarDevolucao).isInstanceOf(TransicaoInvalidaException.class);
        assertThat(reserva.getEstado()).isEqualTo(estado);
    }
}
