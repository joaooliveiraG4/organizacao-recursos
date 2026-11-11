package br.com.senac.recursos.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/** RF-08 — recurso em manutenção não pode ser reservado. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ManutencaoIntegrationTest {

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
    void salaEmManutencaoRecusaReservaEVoltaAAceitarAoSair() throws Exception {
        var admin = cadastrarUsuario("ADMINISTRADOR");
        var solicitante = cadastrarUsuario("SOLICITANTE");
        var sala = cadastrarSala();

        marcar("salas", sala, true, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.emManutencao").value(true));
        reservar(solicitante, sala, null, "2044-01-10").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_recurso WHERE recurso_id = ?",
                Long.class, sala)).isZero();

        marcar("salas", sala, false, admin).andExpect(status().isOk());
        reservar(solicitante, sala, null, "2044-01-10").andExpect(status().isCreated());

        assertThat(jdbc.queryForList("SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number",
                String.class, sala)).containsExactly("MANUTENCAO_INICIADA", "MANUTENCAO_ENCERRADA");
    }

    @Test
    void materialEmManutencaoRecusaReserva() throws Exception {
        var admin = cadastrarUsuario("ADMINISTRADOR");
        var material = UUID.randomUUID();
        jdbc.update("INSERT INTO material (id, codigo, nome, tipo, quantidade) VALUES (?, ?, 'Projetor', 'audiovisual', 1)",
                material, material.toString().substring(0, 20));

        marcar("materiais", material, true, admin).andExpect(status().isOk());

        reservar(cadastrarUsuario("SOLICITANTE"), cadastrarSala(), material, "2044-02-10")
                .andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "{0} não marca manutenção")
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void somenteAdministradorMarcaManutencao(String perfil) throws Exception {
        var sala = cadastrarSala();

        marcar("salas", sala, true, cadastrarUsuario(perfil)).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT em_manutencao FROM sala WHERE id = ?", Boolean.class, sala)).isFalse();
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        marcar("salas", UUID.randomUUID(), true, cadastrarUsuario("ADMINISTRADOR")).andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions marcar(String recurso, UUID id, boolean valor, UUID usuario)
            throws Exception {
        return mvc.perform(put("/api/v1/{recurso}/{id}/manutencao", recurso, id)
                .with(login(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emManutencao\":" + valor + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions reservar(UUID solicitante, UUID sala, UUID material,
            String dia) throws Exception {
        var materiais = material == null ? "" : ",\"materialIds\":[\"" + material + "\"]";
        return mvc.perform(post("/api/v1/reservas")
                .with(login(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"salaId\":\"%s\",\"inicio\":\"%sT10:00:00Z\",\"fim\":\"%sT11:00:00Z\"%s}"
                        .formatted(sala, dia, dia, materiais)));
    }

    private UUID cadastrarSala() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO sala (id, codigo, tipo, capacidade, localizacao) VALUES (?, ?, 'Aula', 20, 'Bloco M')",
                id, id.toString().substring(0, 20));
        return id;
    }

    private UUID cadastrarUsuario(String perfil) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, name, email, role, password_hash) VALUES (?, 'Teste', ?, ?, ?)",
                id, id + "@example.test", perfil, senhas.encode(SENHA));
        return id;
    }

    private static RequestPostProcessor login(UUID usuario) {
        return httpBasic(usuario + "@example.test", SENHA);
    }
}
