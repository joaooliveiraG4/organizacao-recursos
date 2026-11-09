package br.com.senac.recursos.material;

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

import br.com.senac.recursos.catalogo.CatalogoExceptionHandler;
import br.com.senac.recursos.catalogo.RecursoJaCadastradoException;
import br.com.senac.recursos.catalogo.RecursoNaoEncontradoException;
import br.com.senac.recursos.config.SecurityConfiguration;
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

@WebMvcTest(MaterialController.class)
@Import({SecurityConfiguration.class, CatalogoExceptionHandler.class})
@WithMockUser(roles = "ADMINISTRADOR")
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService materialService;

    @ParameterizedTest(name = "{0} não cadastra material")
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfilSemPermissaoRecebe403AoCadastrar(String perfil) throws Exception {
        mockMvc.perform(post("/api/v1/materiais")
                        .with(user("pessoa@exemplo.test").roles(perfil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(materialService);
    }

    @ParameterizedTest(name = "{0} consulta materiais")
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void qualquerPerfilAutenticadoConsulta(String perfil) throws Exception {
        when(materialService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/materiais").with(user("pessoa@exemplo.test").roles(perfil)))
                .andExpect(status().isOk());
    }

    @Test
    void deveCriarMaterialComLocation() throws Exception {
        var material = new MaterialResponse(
                UUID.randomUUID(), "MAT-1", "Projetor", "audiovisual", 2);
        when(materialService.criar(any(CriarMaterialRequest.class))).thenReturn(material);

        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString(material.id().toString())))
                .andExpect(jsonPath("$.quantidade").value(2));
    }

    @Test
    void deveListarMateriais() throws Exception {
        when(materialService.listar()).thenReturn(
                List.of(new MaterialResponse(UUID.randomUUID(), "MAT-1", "Projetor", "audiovisual", 2)));

        mockMvc.perform(get("/api/v1/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MAT-1"));
    }

    @Test
    void deveConsultarMaterialPorId() throws Exception {
        var id = UUID.randomUUID();
        when(materialService.consultar(id))
                .thenReturn(new MaterialResponse(id, "MAT-1", "Projetor", "audiovisual", 2));

        mockMvc.perform(get("/api/v1/materiais/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deveRejeitarQuantidadeZeroAntesDeChamarServico() throws Exception {
        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("quantidade"));

        verifyNoInteractions(materialService);
    }

    @Test
    void deveConverterCodigoDuplicadoEmConflito() throws Exception {
        when(materialService.criar(any(CriarMaterialRequest.class)))
                .thenThrow(new RecursoJaCadastradoException("material", "MAT-1"));

        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveConverterMaterialAusenteEmNaoEncontrado() throws Exception {
        var id = UUID.randomUUID();
        when(materialService.consultar(id))
                .thenThrow(new RecursoNaoEncontradoException("Material", id));

        mockMvc.perform(get("/api/v1/materiais/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveResponderConflitoSeguroParaFalhaDeIntegridade() throws Exception {
        when(materialService.criar(any(CriarMaterialRequest.class)))
                .thenThrow(new DataIntegrityViolationException("detalhe interno"));

        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Não foi possível salvar o recurso porque os dados violam uma regra de integridade do catálogo."));
    }
}
