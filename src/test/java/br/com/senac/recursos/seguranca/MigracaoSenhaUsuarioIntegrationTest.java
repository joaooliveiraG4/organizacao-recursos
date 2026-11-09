package br.com.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A V5 precisa subir em bancos que já tinham usuários da V1, sem senha.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracaoSenhaUsuarioIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void usuarioAnteriorAV5FicaInativoESemSenhaValida() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        var id = UUID.randomUUID();

        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        jdbc.update("INSERT INTO app_user (id, name, email, role) VALUES (?, 'Legado', 'legado@exemplo.test', 'SOLICITANTE')", id);

        Flyway.configure().dataSource(dataSource).load().migrate();

        var linha = jdbc.queryForMap("SELECT password_hash, active FROM app_user WHERE id = ?", id);
        assertThat(linha.get("active")).isEqualTo(false);
        assertThat(new BCryptPasswordEncoder().matches("", (String) linha.get("password_hash"))).isFalse();
    }
}
