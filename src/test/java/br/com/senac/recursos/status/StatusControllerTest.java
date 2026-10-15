package br.com.senac.recursos.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class StatusControllerTest {

    @Test
    void deveInformarServicoDisponivelComHorarioControlado() {
        var instante = Instant.parse("2026-10-15T12:00:00Z");
        var controller = new StatusController(Clock.fixed(instante, ZoneOffset.UTC));

        var resposta = controller.status();

        assertThat(resposta)
                .containsEntry("service", "organizacao-recursos")
                .containsEntry("status", "UP")
                .containsEntry("timestamp", instante);
    }
}

