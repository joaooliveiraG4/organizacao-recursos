package br.com.senac.recursos.relatorio;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** RF-12 — resposta do relatório de utilização. */
public record RelatorioUtilizacao(
        Instant inicio,
        Instant fim,
        Map<String, Long> reservasPorEstado,
        List<UsoRecurso> recursos,
        long conflitosEvitados) {

    /** Para professores, {@code horas} é a carga horária no período. */
    public record UsoRecurso(String tipo, UUID recursoId, String codigo, long reservas, double horas) {
    }
}
