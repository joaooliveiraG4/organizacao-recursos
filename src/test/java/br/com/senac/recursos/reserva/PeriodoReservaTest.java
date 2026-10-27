package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PeriodoReservaTest {

    @ParameterizedTest(name = "{0} até {1}: sobreposição = {2}")
    @CsvSource({
        "12:00, 13:00, false",
        "13:00, 14:00, false",
        "16:00, 17:00, false",
        "17:00, 18:00, false",
        "13:00, 15:00, true",
        "15:00, 17:00, true",
        "14:30, 15:30, true",
        "13:00, 17:00, true",
        "14:00, 16:00, true",
        "14:00, 15:00, true",
        "15:00, 16:00, true"
    })
    void deveDetectarSobreposicaoNosDoisSentidos(String inicio, String fim, boolean esperado) {
        var existente = periodo("14:00", "16:00");
        var nova = periodo(inicio, fim);

        assertThat(existente.sobrepoe(nova)).isEqualTo(esperado);
        assertThat(nova.sobrepoe(existente)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"14:00, 14:00", "16:00, 14:00"})
    void deveRecusarDuracaoNulaOuNegativa(String inicio, String fim) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> periodo(inicio, fim))
                .withMessage("O fim da reserva deve ser posterior ao início");
    }

    @Test
    void deveRecusarInicioAusente() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PeriodoReserva(null, instante("16:00")))
                .withMessage("Informe o início da reserva");
    }

    @Test
    void deveRecusarFimAusente() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PeriodoReserva(instante("14:00"), null))
                .withMessage("Informe o fim da reserva");
    }

    @Test
    void deveRecusarComparacaoSemPeriodo() {
        var existente = periodo("14:00", "16:00");

        assertThatNullPointerException()
                .isThrownBy(() -> existente.sobrepoe(null))
                .withMessage("Informe o período a comparar");
    }

    @Test
    void deveCompararInstantesIndependentementeDoFusoInformado() {
        var existente = periodo("14:00", "16:00");
        var inicioLocal = OffsetDateTime.parse("2026-10-27T12:00:00-03:00").toInstant();
        var fimLocal = OffsetDateTime.parse("2026-10-27T14:00:00-03:00").toInstant();

        assertThat(existente.sobrepoe(new PeriodoReserva(inicioLocal, fimLocal))).isTrue();
    }

    @Test
    void deveDetectarConflitoEntreDiasDiferentes() {
        var existente = new PeriodoReserva(
                Instant.parse("2026-10-27T23:00:00Z"), Instant.parse("2026-10-28T01:00:00Z"));
        var nova = new PeriodoReserva(
                Instant.parse("2026-10-28T00:30:00Z"), Instant.parse("2026-10-28T02:00:00Z"));

        assertThat(existente.sobrepoe(nova)).isTrue();
    }

    @Test
    void devePreservarPrecisaoMenorQueUmSegundo() {
        var existente = periodo("14:00", "16:00");
        var nova = new PeriodoReserva(
                existente.fim().minusNanos(1), existente.fim().plusSeconds(1));

        assertThat(existente.sobrepoe(nova)).isTrue();
    }

    private static PeriodoReserva periodo(String inicio, String fim) {
        return new PeriodoReserva(instante(inicio), instante(fim));
    }

    private static Instant instante(String horario) {
        return Instant.parse("2026-10-27T" + horario + ":00Z");
    }
}