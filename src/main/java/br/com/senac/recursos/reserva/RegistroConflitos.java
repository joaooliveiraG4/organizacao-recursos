package br.com.senac.recursos.reserva;

import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-12 — guarda cada reserva recusada por sobreposição. Usa uma transação
 * própria (REQUIRES_NEW) porque a transação da reserva é desfeita no 409;
 * sem isso, o conflito evitado sumiria junto.
 */
@Component
public class RegistroConflitos {

    private final JdbcTemplate jdbc;

    public RegistroConflitos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tipoRecurso, UUID recursoId, PeriodoReserva periodo) {
        jdbc.update("""
                INSERT INTO conflito_evitado (id, tipo_recurso, recurso_id, periodo_inicio, periodo_fim)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), tipoRecurso, recursoId,
                periodo.inicio().atOffset(ZoneOffset.UTC), periodo.fim().atOffset(ZoneOffset.UTC));
    }
}
