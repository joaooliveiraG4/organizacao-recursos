package br.com.senac.recursos.relatorio;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.senac.recursos.reserva.EstadoReserva;

/**
 * RF-12 — utilização, carga horária e conflitos evitados, calculados direto do
 * banco para não divergir das reservas persistidas. Entra no relatório toda
 * reserva cujo período cruza [inicio, fim). Recursos e horas contam só reservas
 * em SOLICITADA, APROVADA, EM_USO ou CONCLUIDA; canceladas, rejeitadas e não
 * comparecimentos aparecem apenas na contagem por estado.
 */
@Service
public class RelatorioUtilizacaoService {

    private final JdbcTemplate jdbc;

    public RelatorioUtilizacaoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RelatorioUtilizacao gerar(Instant inicio, Instant fim) {
        if (!fim.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O fim do período deve ser posterior ao início.");
        }
        var de = inicio.atOffset(ZoneOffset.UTC);
        var ate = fim.atOffset(ZoneOffset.UTC);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (var estado : EstadoReserva.values()) {
            porEstado.put(estado.name(), 0L);
        }
        jdbc.query("""
                SELECT estado, count(*) AS total FROM reserva
                WHERE inicio < ? AND fim > ?
                GROUP BY estado
                """, linha -> {
            porEstado.put(linha.getString("estado"), linha.getLong("total"));
        }, ate, de);

        var recursos = jdbc.query("""
                SELECT rr.tipo_recurso,
                       rr.recurso_id,
                       COALESCE(s.codigo, p.codigo, m.codigo) AS codigo,
                       count(*) AS reservas,
                       sum(extract(epoch FROM (r.fim - r.inicio))) / 3600.0 AS horas
                FROM reserva_recurso rr
                JOIN reserva r ON r.id = rr.reserva_id
                LEFT JOIN sala s ON rr.tipo_recurso = 'SALA' AND s.id = rr.recurso_id
                LEFT JOIN professor p ON rr.tipo_recurso = 'PROFESSOR' AND p.id = rr.recurso_id
                LEFT JOIN material m ON rr.tipo_recurso = 'MATERIAL' AND m.id = rr.recurso_id
                WHERE r.inicio < ? AND r.fim > ?
                  AND r.estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')
                GROUP BY rr.tipo_recurso, rr.recurso_id, s.codigo, p.codigo, m.codigo
                ORDER BY 1, 3
                """, (linha, n) -> new RelatorioUtilizacao.UsoRecurso(
                        linha.getString("tipo_recurso"),
                        linha.getObject("recurso_id", UUID.class),
                        linha.getString("codigo"),
                        linha.getLong("reservas"),
                        linha.getDouble("horas")), ate, de);

        Long conflitos = jdbc.queryForObject("""
                SELECT count(*) FROM conflito_evitado
                WHERE periodo_inicio < ? AND periodo_fim > ?
                """, Long.class, ate, de);

        return new RelatorioUtilizacao(inicio, fim, porEstado, recursos, conflitos == null ? 0 : conflitos);
    }
}
