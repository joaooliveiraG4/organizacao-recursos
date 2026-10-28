package br.com.senac.recursos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class AuditoriaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final UUID REGISTRO = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();

    private final EventoAuditoriaRepository repository = mock(EventoAuditoriaRepository.class);
    private final AuditoriaService service =
            new AuditoriaService(repository, Clock.fixed(AGORA, ZoneOffset.UTC));

    static Stream<Arguments> identificacaoIncompleta() {
        return Stream.of(
                Arguments.of(null, REGISTRO, "RESERVA_APROVADA", AUTOR, "Informe o tipo do registro auditado"),
                Arguments.of("  ", REGISTRO, "RESERVA_APROVADA", AUTOR, "Informe o tipo do registro auditado"),
                Arguments.of("RESERVA", null, "RESERVA_APROVADA", AUTOR, "Informe o registro auditado"),
                Arguments.of("RESERVA", REGISTRO, null, AUTOR, "Informe a ação auditada"),
                Arguments.of("RESERVA", REGISTRO, "", AUTOR, "Informe a ação auditada"),
                Arguments.of("RESERVA", REGISTRO, "RESERVA_APROVADA", null, "Informe o autor da ação"));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("identificacaoIncompleta")
    void recusaEventoSemIdentificacaoCompleta(
            String tipo, UUID registroId, String acao, UUID autorId, String mensagem) {
        assertThatThrownBy(() -> service.registrar(tipo, registroId, acao, autorId, null))
                .hasMessage(mensagem);

        verify(repository, never()).save(any());
    }

    @Test
    void gravaEventoComHorarioDoRelogioEDetalhesVaziosQuandoAusentes() {
        service.registrar(" RESERVA ", REGISTRO, " RESERVA_CRIADA ", AuditoriaService.AUTOR_SISTEMA, null);

        var captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(repository).save(captor.capture());
        var registro = captor.getValue().paraRegistro();
        assertThat(registro.tipo()).isEqualTo("RESERVA");
        assertThat(registro.acao()).isEqualTo("RESERVA_CRIADA");
        assertThat(registro.autorId()).isEqualTo(new UUID(0L, 0L));
        assertThat(registro.ocorridoEm()).isEqualTo(AGORA);
        assertThat(registro.detalhes()).isEmpty();
    }
}
