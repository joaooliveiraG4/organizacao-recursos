package br.com.senac.recursos.professor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfessorController.class)
@Import({SecurityConfiguration.class, CatalogoExceptionHandler.class})
@WithMockUser
class ProfessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorService professorService;

    @Test
    void deveCriarProfessorComLocation() throws Exception {
        var professor = new ProfessorResponse(
                UUID.randomUUID(), "PROF-1", "Maria Silva", "Matemática");
        when(professorService.criar(any(CriarProfessorRequest.class))).thenReturn(professor);

        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\"Matemática\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString(professor.id().toString())))
                .andExpect(jsonPath("$.competencia").value("Matemática"));
    }

    @Test
    void deveListarProfessores() throws Exception {
        when(professorService.listar()).thenReturn(
                List.of(new ProfessorResponse(UUID.randomUUID(), "PROF-1", "Maria Silva", "Matemática")));

        mockMvc.perform(get("/api/v1/professores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PROF-1"));
    }

    @Test
    void deveConsultarProfessorPorId() throws Exception {
        var id = UUID.randomUUID();
        when(professorService.consultar(id))
                .thenReturn(new ProfessorResponse(id, "PROF-1", "Maria Silva", "Matemática"));

        mockMvc.perform(get("/api/v1/professores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deveRejeitarCompetenciaVaziaAntesDeChamarServico() throws Exception {
        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("competencia"));

        verifyNoInteractions(professorService);
    }

    @Test
    void deveConverterCodigoDuplicadoEmConflito() throws Exception {
        when(professorService.criar(any(CriarProfessorRequest.class)))
                .thenThrow(new RecursoJaCadastradoException("professor", "PROF-1"));

        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\"Matemática\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveConverterProfessorAusenteEmNaoEncontrado() throws Exception {
        var id = UUID.randomUUID();
        when(professorService.consultar(id))
                .thenThrow(new RecursoNaoEncontradoException("Professor", id));

        mockMvc.perform(get("/api/v1/professores/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveResponderConflitoSeguroParaFalhaDeIntegridade() throws Exception {
        when(professorService.criar(any(CriarProfessorRequest.class)))
                .thenThrow(new DataIntegrityViolationException("detalhe interno"));

        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\"Matemática\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Não foi possível salvar o recurso porque os dados violam uma regra de integridade do catálogo."));
    }
}
