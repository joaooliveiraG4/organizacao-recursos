package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

    @Autowired
    private TransactionTemplate transacao;

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void apenasUmaTransacaoConsegueReservarAMesmaSalaNoMesmoHorario() throws Exception {
        var solicitante = cadastrarSolicitante();
        var sala = UUID.randomUUID();
        var primeiraGravou = new CountDownLatch(1);
        var segundaVaiGravar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> tentarReservarSala(solicitante, sala, () -> {}, () -> {
                primeiraGravou.countDown();
                aguardarInicio(segundaVaiGravar);
            }));
            var segunda = executor.submit(() -> tentarReservarSala(solicitante, sala, () -> {
                aguardarInicio(primeiraGravou);
                segundaVaiGravar.countDown();
            }, () -> {}));
            assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("COMMIT", "23P01");
        }

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva_recurso
                WHERE tipo_recurso = 'SALA' AND recurso_id = ? AND ativo
                """, Long.class, sala)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva WHERE solicitante_id = ?
                """, Long.class, solicitante)).isEqualTo(1L);
    }

    private String tentarReservarSala(UUID solicitante, UUID sala, Runnable antesDaAlocacao,
            Runnable depoisDaAlocacao) {
        try {
            transacao.executeWithoutResult(status -> {
                var reserva = new Reserva(solicitante, new PeriodoReserva(INICIO, FIM));
                entidades.persist(reserva);
                entidades.flush();
                jdbc.execute("SET LOCAL statement_timeout = '10s'");
                antesDaAlocacao.run();
                jdbc.update("""
                        INSERT INTO reserva_recurso (reserva_id, tipo_recurso, recurso_id, periodo)
                        VALUES (?, 'SALA', ?, tstzrange(?, ?, '[)'))
                        """, reserva.getId(), sala, INICIO.atOffset(ZoneOffset.UTC),
                        FIM.atOffset(ZoneOffset.UTC));
                depoisDaAlocacao.run();
            });
            return "COMMIT";
        } catch (DataIntegrityViolationException conflito) {
            if (conflito.getMostSpecificCause() instanceof SQLException sql
                    && "23P01".equals(sql.getSQLState())) {
                return sql.getSQLState();
            }
            throw conflito;
        }
    }

    private static void aguardarInicio(CountDownLatch iniciar) {
        try {
            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As transações não foram liberadas a tempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A disputa foi interrompida", e);
        }
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
