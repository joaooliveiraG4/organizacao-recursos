package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * RF-09 — retirada e devolução pela API, com PostgreSQL real. Cobre os
 * cenários U01–U05 e U07 do roteiro de QA do Kauã (PR #9,
 * docs/testes/qa-utilizacao-e-concorrencia.md; o arquivo entra na main com a #9).
 * O U06 (rollback se a auditoria falhar) está em UtilizacaoReservaRollbackIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UtilizacaoReservaIntegrationTest {

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
    void u01RetiradaDeReservaAprovadaPassaParaEmUsoComAuditoriaDoResponsavel() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = reservaAprovada();

        mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_USO"));

        assertThat(estado(reserva)).isEqualTo("EM_USO");
        assertThat(ultimoEvento(reserva)).containsExactly("RETIRADA_REGISTRADA", responsavel.toString());
    }

    @Test
    void u02DevolucaoConcluiReservaELiberaOHorarioParaNovaReserva() throws Exception {
        var administrador = cadastrarUsuario("ADMINISTRADOR");
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var sala = cadastrarSala();
        var periodo = periodoNovo();
        var reserva = criarReserva(solicitante, sala, periodo);
        definirEstado(reserva, EstadoReserva.EM_USO);

        mvc.perform(post("/api/v1/reservas/{id}/devolucao", reserva).with(login(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

        assertThat(estado(reserva)).isEqualTo("CONCLUIDA");
        assertThat(ultimoEvento(reserva)).containsExactly("DEVOLUCAO_REGISTRADA", administrador.toString());
        assertThat(alocacoesAtivas(reserva)).isZero();
        // O horário foi liberado: a mesma sala pode ser reservada de novo no mesmo período.
        criarReserva(solicitante, sala, periodo);
    }

    @Test
    void u03RetiradaEmEstadoIncompativelRetorna409SemMudarNemAuditar() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva(cadastrarUsuario("SOLICITANTE"), cadastrarSala(), periodoNovo());
        var eventosAntes = eventos(reserva).size();

        mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(estado(reserva)).isEqualTo("SOLICITADA");
        assertThat(eventos(reserva)).hasSize(eventosAntes);
    }

    @Test
    void u03DevolucaoSemRetiradaRetorna409() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = reservaAprovada();

        mvc.perform(post("/api/v1/reservas/{id}/devolucao", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(estado(reserva)).isEqualTo("APROVADA");
    }

    @Test
    void u04RetiradaRepetidaRetorna409SemDuplicarMovimentacao() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = reservaAprovada();

        mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(eventos(reserva)).filteredOn("RETIRADA_REGISTRADA"::equals).hasSize(1);
    }

    @Test
    void u05SolicitanteNaoRegistraRetiradaNemDevolucao() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var reserva = reservaAprovada();

        mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(solicitante)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservas/{id}/devolucao", reserva).with(login(solicitante)))
                .andExpect(status().isForbidden());

        assertThat(estado(reserva)).isEqualTo("APROVADA");
    }

    @Test
    void reservaInexistenteRetorna404() throws Exception {
        mvc.perform(post("/api/v1/reservas/{id}/retirada", UUID.randomUUID())
                        .with(login(cadastrarUsuario("RESPONSAVEL"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void u07DuasRetiradasSimultaneasConfirmamApenasUma() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = reservaAprovada();
        var prontas = new CountDownLatch(2);
        var largada = new CountDownLatch(1);

        List<Integer> respostas;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> retirarJuntos(reserva, responsavel, prontas, largada));
            var segunda = executor.submit(() -> retirarJuntos(reserva, responsavel, prontas, largada));
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            respostas = List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
        }

        assertThat(respostas).containsExactlyInAnyOrder(200, 409);
        assertThat(eventos(reserva)).filteredOn("RETIRADA_REGISTRADA"::equals).hasSize(1);
        assertThat(estado(reserva)).isEqualTo("EM_USO");
    }

    private int retirarJuntos(UUID reserva, UUID responsavel, CountDownLatch prontas,
            CountDownLatch largada) throws Exception {
        prontas.countDown();
        if (!largada.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("largada não liberada");
        }
        return mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel)))
                .andReturn().getResponse().getStatus();
    }

    // ---- preparação ----

    private UUID reservaAprovada() throws Exception {
        var reserva = criarReserva(cadastrarUsuario("SOLICITANTE"), cadastrarSala(), periodoNovo());
        // A aprovação é o RF-07; aqui só posicionamos o estado de partida do RF-09.
        definirEstado(reserva, EstadoReserva.APROVADA);
        return reserva;
    }

    private UUID criarReserva(UUID solicitante, UUID sala, Instant[] periodo) throws Exception {
        var resposta = mvc.perform(post("/api/v1/reservas")
                        .with(login(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salaId":"%s","inicio":"%s","fim":"%s"}
                                """.formatted(sala, periodo[0], periodo[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    /** Cada teste usa um dia diferente para não colidir com reservas de outros testes. */
    private static Instant[] periodoNovo() {
        var inicio = Instant.parse("2030-01-01T14:00:00Z")
                .plusSeconds(86_400L * (Math.abs(UUID.randomUUID().getMostSignificantBits()) % 3_000));
        return new Instant[] {inicio, inicio.plusSeconds(7_200)};
    }

    private UUID cadastrarSala() {
        var sala = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sala (id, codigo, tipo, capacidade, localizacao)
                VALUES (?, ?, 'Laboratório', 20, 'Bloco U')
                """, sala, sala.toString().substring(0, 20));
        return sala;
    }

    private UUID cadastrarUsuario(String perfil) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user (id, name, email, role, password_hash)
                VALUES (?, ?, ?, ?, ?)
                """, id, "Usuário de teste", id + "@example.test", perfil, senhas.encode(SENHA));
        return id;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor login(UUID usuario) {
        return httpBasic(usuario + "@example.test", SENHA);
    }

    private void definirEstado(UUID reserva, EstadoReserva estado) {
        jdbc.update("UPDATE reserva SET estado = ? WHERE id = ?", estado.name(), reserva);
    }

    private String estado(UUID reserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reserva);
    }

    private long alocacoesAtivas(UUID reserva) {
        return jdbc.queryForObject("SELECT count(*) FROM reserva_recurso WHERE reserva_id = ? AND ativo",
                Long.class, reserva);
    }

    private List<String> eventos(UUID reserva) {
        return jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva);
    }

    private List<String> ultimoEvento(UUID reserva) {
        return jdbc.queryForObject("""
                SELECT action, actor_id FROM audit_event WHERE aggregate_id = ?
                ORDER BY sequence_number DESC LIMIT 1
                """, (linha, n) -> List.of(linha.getString("action"), linha.getString("actor_id")), reserva);
    }
}
