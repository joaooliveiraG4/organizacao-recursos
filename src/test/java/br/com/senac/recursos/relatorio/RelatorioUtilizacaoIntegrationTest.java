package br.com.senac.recursos.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * RF-12 — o relatório confere com o que está persistido. Risco coberto:
 * relatório divergir das reservas (contar cancelada, perder horas, não contar
 * conflito evitado). Cada teste usa um ano próprio para isolar os dados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RelatorioUtilizacaoIntegrationTest {

    private static final String SENHA = "senha-de-teste";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder senhas;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void totaisConferemComAsReservasPersistidas() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var sala = cadastrarSala("REL-SALA-1");
        var professor = cadastrarProfessor("REL-PROF-1");

        // Sala: 2 h + 3 h válidas; 1 h cancelada não conta. Professor: 2 h.
        criarReserva(solicitante, sala, professor, "2041-03-02T10:00:00Z", "2041-03-02T12:00:00Z");
        criarReserva(solicitante, sala, null, "2041-03-03T08:00:00Z", "2041-03-03T11:00:00Z");
        var cancelada = criarReserva(solicitante, sala, null, "2041-03-04T08:00:00Z", "2041-03-04T09:00:00Z");
        mvc.perform(delete("/api/v1/reservas/{id}", cancelada).with(login(solicitante)))
                .andExpect(status().isNoContent());
        // Fora do período do relatório: não conta.
        criarReserva(solicitante, sala, null, "2041-05-01T08:00:00Z", "2041-05-01T09:00:00Z");

        mvc.perform(get("/api/v1/relatorios/utilizacao")
                        .param("inicio", "2041-03-01T00:00:00Z")
                        .param("fim", "2041-04-01T00:00:00Z")
                        .with(login(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasPorEstado.APROVADA").value(2))
                .andExpect(jsonPath("$.reservasPorEstado.CANCELADA").value(1))
                .andExpect(jsonPath("$.recursos[?(@.codigo == 'REL-SALA-1')].reservas").value(2))
                .andExpect(jsonPath("$.recursos[?(@.codigo == 'REL-SALA-1')].horas").value(5.0))
                .andExpect(jsonPath("$.recursos[?(@.codigo == 'REL-PROF-1')].tipo").value("PROFESSOR"))
                .andExpect(jsonPath("$.recursos[?(@.codigo == 'REL-PROF-1')].horas").value(2.0))
                .andExpect(jsonPath("$.conflitosEvitados").value(0));
    }

    @Test
    void reservaRecusadaPorSobreposicaoEContadaComoConflitoEvitado() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var administrador = cadastrarUsuario("ADMINISTRADOR");
        var sala = cadastrarSala("REL-SALA-2");
        criarReserva(solicitante, sala, null, "2042-06-10T14:00:00Z", "2042-06-10T16:00:00Z");

        mvc.perform(post("/api/v1/reservas")
                        .with(login(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(sala, null, "2042-06-10T15:00:00Z", "2042-06-10T17:00:00Z")))
                .andExpect(status().isConflict());

        var relatorio = mvc.perform(get("/api/v1/relatorios/utilizacao")
                        .param("inicio", "2042-06-01T00:00:00Z")
                        .param("fim", "2042-07-01T00:00:00Z")
                        .with(login(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflitosEvitados").value(1))
                .andExpect(jsonPath("$.recursos[?(@.codigo == 'REL-SALA-2')].reservas").value(1))
                .andReturn().getResponse().getContentAsString();

        // A recusa não deixou reserva nem alocação: só o registro do conflito.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva_recurso WHERE recurso_id = ?
                """, Long.class, sala)).isEqualTo(1L);
        assertThat(relatorio).doesNotContain(solicitante.toString());
    }

    @Test
    void solicitanteNaoAcessaRelatorio() throws Exception {
        mvc.perform(get("/api/v1/relatorios/utilizacao")
                        .param("inicio", "2043-01-01T00:00:00Z")
                        .param("fim", "2043-02-01T00:00:00Z")
                        .with(login(cadastrarUsuario("SOLICITANTE"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "período {0} → {1} é recusado")
    @CsvSource({
            "2043-02-01T00:00:00Z, 2043-01-01T00:00:00Z",
            "2043-01-01T00:00:00Z, 2043-01-01T00:00:00Z"})
    void periodoInvalidoRetorna400(String inicio, String fim) throws Exception {
        mvc.perform(get("/api/v1/relatorios/utilizacao")
                        .param("inicio", inicio)
                        .param("fim", fim)
                        .with(login(cadastrarUsuario("ADMINISTRADOR"))))
                .andExpect(status().isBadRequest());
    }

    // ---- preparação ----

    private UUID criarReserva(UUID solicitante, UUID sala, UUID professor, String inicio, String fim)
            throws Exception {
        var resposta = mvc.perform(post("/api/v1/reservas")
                        .with(login(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(sala, professor, inicio, fim)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    private static String pedido(UUID sala, UUID professor, String inicio, String fim) {
        var professorJson = professor == null ? "" : ",\"professorId\":\"" + professor + "\"";
        return "{\"salaId\":\"%s\",\"inicio\":\"%s\",\"fim\":\"%s\"%s}"
                .formatted(sala, Instant.parse(inicio), Instant.parse(fim), professorJson);
    }

    private UUID cadastrarSala(String codigo) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sala (id, codigo, tipo, capacidade, localizacao)
                VALUES (?, ?, 'Laboratório', 20, 'Bloco R')
                """, id, codigo);
        return id;
    }

    private UUID cadastrarProfessor(String codigo) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO professor (id, codigo, nome, competencia)
                VALUES (?, ?, 'Professor de teste', 'Química')
                """, id, codigo);
        return id;
    }

    private UUID cadastrarUsuario(String perfil) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user (id, name, email, role, password_hash)
                VALUES (?, ?, ?, ?, ?)
                """, id, "Usuário de teste", id + "@example.test", perfil, senhas.encode(SENHA));
        return id;
    }

    private static RequestPostProcessor login(UUID usuario) {
        return httpBasic(usuario + "@example.test", SENHA);
    }
}
