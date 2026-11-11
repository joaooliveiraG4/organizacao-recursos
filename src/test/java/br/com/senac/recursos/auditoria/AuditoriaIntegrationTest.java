package br.com.senac.recursos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * RF-10 — garantias da auditoria no PostgreSQL real. Cada teste usa um
 * registro novo porque a tabela não aceita limpeza (é imutável por design).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(AuditoriaIntegrationTest.RelogioFixo.class)
class AuditoriaIntegrationTest {

    private static final Instant AGORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final UUID RITA = UUID.randomUUID();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private AuditoriaService auditoria;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void historicoRetornaEventosNaOrdemEmQueAconteceram() {
        var reserva = UUID.randomUUID();

        transacao.executeWithoutResult(status -> {
            auditoria.registrar("RESERVA", reserva, "RESERVA_CRIADA", RITA, Map.of("para", "SOLICITADA"));
            auditoria.registrar("RESERVA", reserva, "RESERVA_APROVADA", RITA,
                    Map.of("de", "SOLICITADA", "para", "APROVADA"));
            auditoria.registrar("RESERVA", UUID.randomUUID(), "RESERVA_CRIADA", RITA, Map.of());
        });

        var historico = auditoria.historico("RESERVA", reserva);

        assertThat(historico)
                .extracting(RegistroAuditoria::acao)
                .containsExactly("RESERVA_CRIADA", "RESERVA_APROVADA");
        var aprovacao = historico.get(1);
        assertThat(aprovacao.autorId()).isEqualTo(RITA);
        assertThat(aprovacao.ocorridoEm()).isEqualTo(AGORA);
        assertThat(aprovacao.detalhes()).containsEntry("de", "SOLICITADA").containsEntry("para", "APROVADA");
    }

    @Test
    void falhaNaOperacaoDesfazTambemOEventoDeAuditoria() {
        var reserva = UUID.randomUUID();

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            auditoria.registrar("RESERVA", reserva, "RESERVA_APROVADA", RITA, Map.of());
            throw new IllegalStateException("falha simulada ao gravar a aprovação");
        })).hasMessage("falha simulada ao gravar a aprovação");

        assertThat(auditoria.historico("RESERVA", reserva)).isEmpty();
    }

    @Test
    void registrarForaDeUmaTransacaoEhRecusado() {
        var reserva = UUID.randomUUID();

        assertThatThrownBy(() -> auditoria.registrar("RESERVA", reserva, "RESERVA_APROVADA", RITA, Map.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditoria.historico("RESERVA", reserva)).isEmpty();
    }

    @ParameterizedTest(name = "banco recusa: {0}")
    @ValueSource(strings = {
        "UPDATE audit_event SET action = 'ADULTERADA' WHERE aggregate_id = ?",
        "DELETE FROM audit_event WHERE aggregate_id = ?",
    })
    void bancoRecusaAlterarOuApagarEvento(String sql) {
        var reserva = UUID.randomUUID();
        transacao.executeWithoutResult(status ->
                auditoria.registrar("RESERVA", reserva, "RESERVA_CANCELADA", RITA, Map.of()));

        assertThatThrownBy(() -> jdbc.update(sql, reserva))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("audit_event é imutável");
        assertThat(auditoria.historico("RESERVA", reserva))
                .extracting(RegistroAuditoria::acao)
                .containsExactly("RESERVA_CANCELADA");
    }

    @Test
    void bancoRecusaEsvaziarATabela() {
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit_event"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("audit_event é imutável");
    }

    @Test
    void bancoRecusaEventoSemAutor() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO audit_event (id, aggregate_type, aggregate_id, action) VALUES (?, 'RESERVA', ?, 'X')",
                UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("actor_id");
    }

    @TestConfiguration
    static class RelogioFixo {

        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }
    }
}
