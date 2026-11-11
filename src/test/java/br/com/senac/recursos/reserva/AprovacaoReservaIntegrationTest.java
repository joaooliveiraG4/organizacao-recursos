package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.senac.recursos.notificacao.Notificacao;
import br.com.senac.recursos.notificacao.TipoNotificacao;
import br.com.senac.recursos.seguranca.Perfil;

/** RF-07: aprovação de reserva restrita apenas pelo perfil Responsável. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@RecordApplicationEvents
class AprovacaoReservaIntegrationTest {

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

    @Autowired
    private ApplicationEvents eventos;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void recursoRestritoFicaSolicitadoAteResponsavelAprovarEAuditaAutor() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reserva.toString()))
                .andExpect(jsonPath("$.estado").value("APROVADA"));

        assertThat(estado(reserva)).isEqualTo("APROVADA");
        assertThat(jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva)).containsExactly("RESERVA_CRIADA", "RESERVA_APROVADA");
        assertThat(jdbc.queryForObject("""
                SELECT actor_id FROM audit_event WHERE aggregate_id = ? AND action = 'RESERVA_APROVADA'
                """, UUID.class, reserva)).isEqualTo(responsavel);
    }

    @Test
    void recursoSemRestricaoJaNasceAprovadoSemAprovacaoManual() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var reserva = criarReserva(solicitante, cadastrarSala(false), "APROVADA");

        assertThat(estado(reserva)).isEqualTo("APROVADA");
        assertThat(jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva)).containsExactly("RESERVA_CRIADA");
    }

    @ParameterizedTest(name = "perfil {0} recebe 403 ao aprovar")
    @EnumSource(value = Perfil.class, names = "RESPONSAVEL", mode = EnumSource.Mode.EXCLUDE)
    void somenteResponsavelPodeAprovar(Perfil perfil) throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var outroPerfil = cadastrarUsuario(perfil.name());
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(outroPerfil)))
                .andExpect(status().isForbidden());

        assertThat(estado(reserva)).isEqualTo("SOLICITADA");
        assertThat(jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva)).containsExactly("RESERVA_CRIADA");
    }

    @Test
    void requisicaoSemAutenticacaoRecebe401() throws Exception {
        mvc.perform(post("/api/v1/reservas/{id}/aprovar", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aprovacaoRepetidaRetorna409SemDuplicarEvento() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(responsavel)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(estado(reserva)).isEqualTo("APROVADA");
        assertThat(jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva)).containsExactly("RESERVA_CRIADA", "RESERVA_APROVADA");
    }

    @ParameterizedTest(name = "aprovação recusada quando reserva está em {0}")
    @EnumSource(value = EstadoReserva.class, names = "SOLICITADA", mode = EnumSource.Mode.EXCLUDE)
    void estadoDiferenteDeSolicitadaRetorna409SemAuditar(EstadoReserva estado) throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");
        jdbc.update("UPDATE reserva SET estado = ? WHERE id = ?", estado.name(), reserva);

        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(estado(reserva)).isEqualTo(estado.name());
        assertThat(jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, reserva)).containsExactly("RESERVA_CRIADA");
    }

    @Test
    void reservaInexistenteRetorna404() throws Exception {
        mvc.perform(post("/api/v1/reservas/{id}/aprovar", UUID.randomUUID())
                        .with(login(cadastrarUsuario("RESPONSAVEL"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void aprovacaoPublicaNotificacaoParaOSolicitante() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(cadastrarUsuario("RESPONSAVEL"))))
                .andExpect(status().isOk());

        assertThat(eventos.stream(Notificacao.class))
                .containsExactly(new Notificacao(solicitante, TipoNotificacao.RESERVA_APROVADA, reserva));
    }

    @Test
    void rejeicaoLiberaOHorarioAuditaENotifica() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var sala = cadastrarSala(true);
        var reserva = criarReserva(solicitante, sala, "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/rejeitar", reserva).with(login(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REJEITADA"));

        assertThat(estado(reserva)).isEqualTo("REJEITADA");
        assertThat(jdbc.queryForObject("""
                SELECT actor_id FROM audit_event WHERE aggregate_id = ? AND action = 'RESERVA_REJEITADA'
                """, UUID.class, reserva)).isEqualTo(responsavel);
        assertThat(eventos.stream(Notificacao.class))
                .containsExactly(new Notificacao(solicitante, TipoNotificacao.RESERVA_REJEITADA, reserva));
        // Horário liberado: a mesma sala pode ser pedida de novo no mesmo período.
        criarReserva(solicitante, sala, "SOLICITADA");
    }

    @Test
    void solicitanteNaoRejeitaEReservaAprovadaNaoERejeitada() throws Exception {
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva(solicitante, cadastrarSala(true), "SOLICITADA");

        mvc.perform(post("/api/v1/reservas/{id}/rejeitar", reserva).with(login(solicitante)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservas/{id}/aprovar", reserva).with(login(responsavel)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/reservas/{id}/rejeitar", reserva).with(login(responsavel)))
                .andExpect(status().isConflict());

        assertThat(estado(reserva)).isEqualTo("APROVADA");
        assertThat(eventos.stream(Notificacao.class).map(Notificacao::tipo))
                .containsExactly(TipoNotificacao.RESERVA_APROVADA);
    }

    private UUID criarReserva(UUID solicitante, UUID sala, String estadoEsperado) throws Exception {
        var response = mvc.perform(post("/api/v1/reservas")
                        .with(login(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salaId":"%s","inicio":"2040-01-01T14:00:00Z","fim":"2040-01-01T16:00:00Z"}
                                """.formatted(sala)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value(estadoEsperado))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private UUID cadastrarSala(boolean restrita) {
        var sala = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sala (id, codigo, tipo, capacidade, localizacao, restrito)
                VALUES (?, ?, 'Sala de aula', 20, 'Bloco A', ?)
                """, sala, sala.toString().substring(0, 20), restrita);
        return sala;
    }

    private UUID cadastrarUsuario(String perfil) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user (id, name, email, role, password_hash)
                VALUES (?, 'Usuário de teste', ?, ?, ?)
                """, id, id + "@example.test", perfil, senhas.encode(SENHA));
        return id;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor login(UUID usuario) {
        return httpBasic(usuario + "@example.test", SENHA);
    }

    private String estado(UUID reserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reserva);
    }
}
