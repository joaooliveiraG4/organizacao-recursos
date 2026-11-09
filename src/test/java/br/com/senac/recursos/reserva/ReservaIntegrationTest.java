package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class ReservaIntegrationTest {

    private static final Instant INICIO = Instant.parse("2026-11-12T14:00:00Z");
    private static final Instant FIM = Instant.parse("2026-11-12T16:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private EntityManager entidades;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void gravaEReleReservaSolicitada() {
        var solicitante = cadastrarSolicitante();
        var periodo = new PeriodoReserva(INICIO, FIM);
        var reserva = new Reserva(solicitante, periodo);

        entidades.persist(reserva);
        entidades.flush();
        entidades.clear();

        var gravada = entidades.find(Reserva.class, reserva.getId());
        assertThat(gravada.getSolicitanteId()).isEqualTo(solicitante);
        assertThat(gravada.getPeriodo()).isEqualTo(periodo);
        assertThat(gravada.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    void bancoRecusaPeriodoVazioMesmoSemPassarPeloDominio() {
        var solicitante = cadastrarSolicitante();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO reserva (id, solicitante_id, inicio, fim)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), solicitante, INICIO.atOffset(ZoneOffset.UTC),
                INICIO.atOffset(ZoneOffset.UTC)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reserva_periodo");
    }

    private UUID cadastrarSolicitante() {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user (id, name, email, role)
                VALUES (?, ?, ?, ?)
                """, id, "Solicitante de teste", id + "@example.test", "SOLICITANTE");
        return id;
    }
}
