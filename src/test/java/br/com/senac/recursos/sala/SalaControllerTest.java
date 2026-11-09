package br.com.senac.recursos.sala;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.senac.recursos.config.SecurityConfiguration;

@WebMvcTest(SalaController.class)
@Import({SecurityConfiguration.class, SalaExceptionHandler.class})
@WithMockUser(roles = "ADMINISTRADOR")
class SalaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalaService salaService;

    @ParameterizedTest(name = "{0} não cadastra sala")
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfilSemPermissaoRecebe403AoCadastrar(String perfil) throws Exception {
        mockMvc.perform(post("/api/v1/salas")
                        .with(user("pessoa@exemplo.test").roles(perfil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"B-101\",\"tipo\":\"Aula\","
                                + "\"capacidade\":30,\"localizacao\":\"Bloco B\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(salaService);
    }

    @ParameterizedTest(name = "{0} consulta salas")
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void qualquerPerfilAutenticadoConsulta(String perfil) throws Exception {
        when(salaService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/salas").with(user("pessoa@exemplo.test").roles(perfil)))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarSalaCriadaComLocation() throws Exception {
        var sala = new SalaResponse(
                UUID.randomUUID(), "B-101", "Aula", 30, "Bloco B");
        when(salaService.criar(any(CriarSalaRequest.class))).thenReturn(sala);

        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"B-101\",\"tipo\":\"Aula\","
                                + "\"capacidade\":30,\"localizacao\":\"Bloco B\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(sala.id().toString())))
                .andExpect(jsonPath("$.codigo").value("B-101"));
    }

    @Test
    void deveListarSalas() throws Exception {
        when(salaService.listar()).thenReturn(
                List.of(new SalaResponse(UUID.randomUUID(), "B-101", "Aula", 30, "Bloco B")));

        mockMvc.perform(get("/api/v1/salas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("B-101"));
    }

    @Test
    void deveConsultarSalaPorId() throws Exception {
        var id = UUID.randomUUID();
        when(salaService.consultar(id))
                .thenReturn(new SalaResponse(id, "B-101", "Aula", 30, "Bloco B"));

        mockMvc.perform(get("/api/v1/salas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deveRejeitarEntradaInvalidaAntesDeChamarServico() throws Exception {
        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"B-102\",\"tipo\":\"Aula\","
                                + "\"capacidade\":0,\"localizacao\":\"Bloco B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("capacidade"));

        verifyNoInteractions(salaService);
    }

    @Test
    void deveConverterCodigoDuplicadoEmConflitoHttp() throws Exception {
        when(salaService.criar(any(CriarSalaRequest.class)))
                .thenThrow(new SalaJaCadastradaException("B-101"));

        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"B-101\",\"tipo\":\"Aula\","
                                + "\"capacidade\":30,\"localizacao\":\"Bloco B\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Já existe uma sala cadastrada com o código B-101."));
    }

    @Test
    void deveConverterSalaAusenteEmNaoEncontradoHttp() throws Exception {
        var id = UUID.randomUUID();
        when(salaService.consultar(id)).thenThrow(new SalaNaoEncontradaException(id));

        mockMvc.perform(get("/api/v1/salas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarConflitoSeguroParaFalhaDeIntegridade() throws Exception {
        when(salaService.criar(any(CriarSalaRequest.class)))
                .thenThrow(new DataIntegrityViolationException("detalhe interno"));

        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"B-101\",\"tipo\":\"Aula\","
                                + "\"capacidade\":30,\"localizacao\":\"Bloco B\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Não foi possível concluir a operação porque os dados conflitam com um registro existente."));
    }
}
