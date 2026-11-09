package br.com.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.senac.recursos.config.SecurityConfiguration;
import br.com.senac.recursos.config.TimeConfiguration;
import br.com.senac.recursos.status.StatusController;

/**
 * RF-01 — matriz perfil × operação. Rotas do catálogo ainda sem controller
 * nesta branch respondem 404 quando autorizadas; o que se verifica aqui é a
 * decisão da camada de segurança (não ser 401/403).
 */
@WebMvcTest({UsuarioAtualController.class, StatusController.class})
@Import({SecurityConfiguration.class, TimeConfiguration.class})
class AutorizacaoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} {1} por {2} -> permitido={3}")
    @CsvSource({
        "GET,    /api/v1/salas,             SOLICITANTE,   true",
        "GET,    /api/v1/salas,             RESPONSAVEL,   true",
        "GET,    /api/v1/salas,             ADMINISTRADOR, true",
        "GET,    /api/v1/professores,       SOLICITANTE,   true",
        "GET,    /api/v1/materiais,         SOLICITANTE,   true",
        "POST,   /api/v1/salas,             SOLICITANTE,   false",
        "POST,   /api/v1/salas,             RESPONSAVEL,   false",
        "POST,   /api/v1/salas,             ADMINISTRADOR, true",
        "PUT,    /api/v1/salas/123,         RESPONSAVEL,   false",
        "PUT,    /api/v1/salas/123,         ADMINISTRADOR, true",
        "DELETE, /api/v1/salas/123,         SOLICITANTE,   false",
        "DELETE, /api/v1/salas/123,         ADMINISTRADOR, true",
        "POST,   /api/v1/professores,       RESPONSAVEL,   false",
        "POST,   /api/v1/professores,       ADMINISTRADOR, true",
        "PATCH,  /api/v1/materiais/123,     SOLICITANTE,   false",
        "PATCH,  /api/v1/materiais/123,     ADMINISTRADOR, true",
    })
    void deveAplicarMatrizDePermissoesPorPerfil(
            String metodo, String rota, Perfil perfil, boolean permitido) throws Exception {
        var resposta = mockMvc.perform(request(HttpMethod.valueOf(metodo), rota)
                        .with(user(autenticado(perfil)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse();

        if (permitido) {
            assertThat(resposta.getStatus()).isNotIn(401, 403);
        } else {
            assertThat(resposta.getStatus()).isEqualTo(403);
            assertThat(resposta.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            assertThat(resposta.getContentAsString()).contains(RespostaAcessoNegado.SEM_PERMISSAO);
        }
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void todoPerfilAutenticadoConsultaOProprioUsuario(Perfil perfil) throws Exception {
        var usuario = autenticado(perfil);

        mockMvc.perform(get("/api/v1/me").with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.id().toString()))
                .andExpect(jsonPath("$.perfil").value(perfil.name()))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @ParameterizedTest(name = "{0} {1} sem credencial -> 401")
    @CsvSource({
        "GET,    /api/v1/me",
        "GET,    /api/v1/salas",
        "POST,   /api/v1/salas",
        "DELETE, /api/v1/materiais/123",
    })
    void semCredencialDeveResponder401AntesDeAvaliarPerfil(String metodo, String rota) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), rota))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(RespostaAcessoNegado.NAO_AUTENTICADO));
    }

    @Test
    void rotaPublicaDeStatusContinuaAberta() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk());
    }

    private static UsuarioAutenticado autenticado(Perfil perfil) {
        return new UsuarioAutenticado(
                UUID.randomUUID(),
                "Pessoa " + perfil.name(),
                perfil.name().toLowerCase() + "@exemplo.test",
                perfil,
                "{noop}nao-usado",
                true);
    }
}
