package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.senac.recursos.auditoria.AuditoriaService;

/**
 * RF-09, cenário U06 — se a auditoria falhar durante a retirada ou a devolução,
 * nada é confirmado: o estado da reserva, as alocações e o histórico ficam como
 * estavam (ADR 0003). A falha é forçada com um spy no AuditoriaService.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UtilizacaoReservaRollbackIntegrationTest {

    private static final String SENHA = "senha-de-teste";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @MockitoSpyBean
    private AuditoriaService auditoria;

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
    void falhaNaAuditoriaDesfazARetirada() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva("2031-02-03T10:00:00Z");
        definirEstado(reserva, EstadoReserva.APROVADA);
        var eventosAntes = eventos(reserva);
        falharAuditoriaDe("RETIRADA_REGISTRADA");

        assertThatThrownBy(() -> mvc.perform(post("/api/v1/reservas/{id}/retirada", reserva).with(login(responsavel))))
                .hasRootCauseMessage("falha simulada na auditoria");

        assertThat(estado(reserva)).isEqualTo("APROVADA");
        assertThat(eventos(reserva)).isEqualTo(eventosAntes);
    }

    @Test
    void falhaNaAuditoriaDesfazADevolucaoEMantemOHorarioBloqueado() throws Exception {
        var responsavel = cadastrarUsuario("RESPONSAVEL");
        var reserva = criarReserva("2031-02-04T10:00:00Z");
        definirEstado(reserva, EstadoReserva.EM_USO);
        var eventosAntes = eventos(reserva);
        falharAuditoriaDe("DEVOLUCAO_REGISTRADA");

        assertThatThrownBy(() -> mvc.perform(post("/api/v1/reservas/{id}/devolucao", reserva).with(login(responsavel))))
                .hasRootCauseMessage("falha simulada na auditoria");

        assertThat(estado(reserva)).isEqualTo("EM_USO");
        assertThat(eventos(reserva)).isEqualTo(eventosAntes);
        // O UPDATE que liberaria o horário também foi desfeito.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM reserva_recurso WHERE reserva_id = ? AND ativo", Long.class, reserva))
                .isEqualTo(1L);
    }

    private void falharAuditoriaDe(String acao) {
        doThrow(new IllegalStateException("falha simulada na auditoria"))
                .when(AopTestUtils.<AuditoriaService>getUltimateTargetObject(auditoria))
                .registrar(anyString(), any(UUID.class), eq(acao), any(UUID.class), anyMap());
    }

    private UUID criarReserva(String inicio) throws Exception {
        var sala = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sala (id, codigo, tipo, capacidade, localizacao)
                VALUES (?, ?, 'Laboratório', 20, 'Bloco U')
                """, sala, sala.toString().substring(0, 20));
        var comeco = Instant.parse(inicio);
        var resposta = mvc.perform(post("/api/v1/reservas")
                        .with(login(cadastrarUsuario("SOLICITANTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salaId":"%s","inicio":"%s","fim":"%s"}
                                """.formatted(sala, comeco, comeco.plusSeconds(3_600))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
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

    private void definirEstado(UUID reserva, EstadoReserva estado) {
        jdbc.update("UPDATE reserva SET estado = ? WHERE id = ?", estado.name(), reserva);
    }

    private String estado(UUID reserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reserva);
    }

    private java.util.List<String> eventos(UUID reserva) {
        return jdbc.queryForList("SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number",
                String.class, reserva);
    }
}
