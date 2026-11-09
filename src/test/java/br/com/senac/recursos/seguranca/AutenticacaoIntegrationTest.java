package br.com.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * RF-01 — autenticação contra usuários persistidos no PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AutenticacaoIntegrationTest {

    private static final String SENHA = "senha-de-teste-123";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void credencialValidaAutenticaERetornaPerfil() throws Exception {
        criar("ana@exemplo.test", Perfil.SOLICITANTE, true);

        mockMvc.perform(get("/api/v1/me").with(httpBasic("ANA@exemplo.test", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@exemplo.test"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"));
    }

    @Test
    void senhaErradaUsuarioInexistenteEInativoRecebemMesmaResposta401() throws Exception {
        criar("ana@exemplo.test", Perfil.SOLICITANTE, true);
        criar("bruno@exemplo.test", Perfil.ADMINISTRADOR, false);

        var senhaErrada = corpo401("ana@exemplo.test", "senha-errada");
        var inexistente = corpo401("ninguem@exemplo.test", SENHA);
        var inativo = corpo401("bruno@exemplo.test", SENHA);

        assertThat(senhaErrada)
                .contains(RespostaAcessoNegado.NAO_AUTENTICADO)
                .isEqualTo(inexistente)
                .isEqualTo(inativo);
    }

    @Test
    void senhaNuncaEhPersistidaEmTextoPuro() {
        var usuario = criar("ana@exemplo.test", Perfil.SOLICITANTE, true);

        var hash = usuarioRepository.findById(usuario.getId()).orElseThrow().getPasswordHash();

        assertThat(hash).startsWith("$2").doesNotContain(SENHA);
        assertThat(passwordEncoder.matches(SENHA, hash)).isTrue();
    }

    private String corpo401(String email, String senha) throws Exception {
        return mockMvc.perform(get("/api/v1/me").with(httpBasic(email, senha)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private Usuario criar(String email, Perfil perfil, boolean ativo) {
        var usuario = new Usuario("Pessoa de teste", email, perfil,
                passwordEncoder.encode(SENHA), Instant.parse("2026-10-27T12:00:00Z"));
        if (!ativo) {
            usuario.desativar();
        }
        return usuarioRepository.saveAndFlush(usuario);
    }
}
