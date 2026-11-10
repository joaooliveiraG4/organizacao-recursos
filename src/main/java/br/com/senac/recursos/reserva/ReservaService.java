package br.com.senac.recursos.reserva;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.senac.recursos.auditoria.AuditoriaService;

@Service
public class ReservaService {

    private final EntityManager entidades;
    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;

    public ReservaService(EntityManager entidades, JdbcTemplate jdbc, AuditoriaService auditoria) {
        this.entidades = entidades;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
    }

    @Transactional
    public Reserva criar(UUID solicitante, Instant inicio, Instant fim, UUID sala, UUID professor,
            Set<UUID> materiais) {
        PeriodoReserva periodo;
        try {
            periodo = new PeriodoReserva(inicio, fim);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        var reserva = new Reserva(solicitante, periodo);
        entidades.persist(reserva);
        entidades.flush();

        // Todas as criações bloqueiam os recursos na mesma ordem.
        alocar(reserva, "SALA", "sala", sala);
        if (professor != null) {
            alocar(reserva, "PROFESSOR", "professor", professor);
        }
        if (materiais != null) {
            for (UUID material : materiais.stream().sorted().toList()) {
                alocar(reserva, "MATERIAL", "material", material);
            }
        }

        auditoria.registrar("RESERVA", reserva.getId(), "RESERVA_CRIADA", solicitante,
                Map.of("para", reserva.getEstado().name()));
        return reserva;
    }

    private void alocar(Reserva reserva, String tipo, String tabela, UUID recurso) {
        // O nome da tabela é definido pelas chamadas acima. O id é parametrizado.
        var encontrados = jdbc.queryForList("SELECT id FROM " + tabela + " WHERE id = ? FOR NO KEY UPDATE",
                UUID.class, recurso);
        if (encontrados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
        }

        var periodo = reserva.getPeriodo();
        try {
            jdbc.update("""
                    INSERT INTO reserva_recurso (reserva_id, tipo_recurso, recurso_id, periodo)
                    VALUES (?, ?, ?, tstzrange(?, ?, '[)'))
                    """, reserva.getId(), tipo, recurso, periodo.inicio().atOffset(ZoneOffset.UTC),
                    periodo.fim().atOffset(ZoneOffset.UTC));
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException sql && "23P01".equals(sql.getSQLState())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Recurso indisponível no período.");
            }
            throw e;
        }
    }
}
