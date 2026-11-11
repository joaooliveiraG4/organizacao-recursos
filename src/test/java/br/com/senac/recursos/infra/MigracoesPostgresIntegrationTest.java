package br.com.senac.recursos.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de fumaça estrito: sem {@code disabledWithoutDocker}, a suíte falha
 * quando o Docker não está disponível, em vez de pular os testes de
 * integração em silêncio e terminar verde.
 */
@SpringBootTest
@Testcontainers
class MigracoesPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void todasAsMigracoesSaoAplicadasNoPostgresReal() {
        var migracoes = flyway.info();

        assertThat(migracoes.pending()).isEmpty();
        assertThat(migracoes.applied()).isNotEmpty();
    }
}
