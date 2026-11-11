package br.com.senac.recursos.professor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import br.com.senac.recursos.material.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@WithMockUser(roles = "ADMINISTRADOR")
class ProfessorMaterialApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void limparCatalogos() {
        materialRepository.deleteAll();
        professorRepository.deleteAll();
    }

    @Test
    void deveCadastrarListarEConsultarProfessor() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\"Matemática\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/professores/")))
                .andExpect(jsonPath("$.competencia").value("Matemática"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        mockMvc.perform(get("/api/v1/professores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PROF-1"));
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    void deveCadastrarListarEConsultarMaterial() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/materiais/")))
                .andExpect(jsonPath("$.quantidade").value(2))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        mockMvc.perform(get("/api/v1/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MAT-1"));
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Projetor"));
    }

    @Test
    void deveRejeitarCompetenciaVaziaSemPersistirProfessor() throws Exception {
        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("competencia"));

        assertThat(professorRepository.count()).isZero();
    }

    @Test
    void deveRejeitarQuantidadeInvalidaSemPersistirMaterial() throws Exception {
        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("quantidade"));

        assertThat(materialRepository.count()).isZero();
    }

    @Test
    void deveImpedirCodigosDuplicadosComDiferencaDeCaixa() throws Exception {
        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROF-1\",\"nome\":\"Maria Silva\","
                                + "\"competencia\":\"Matemática\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/professores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"prof-1\",\"nome\":\"Outra Pessoa\","
                                + "\"competencia\":\"História\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"mat-1\",\"nome\":\"Outro Projetor\","
                                + "\"tipo\":\"audiovisual\",\"quantidade\":1}"))
                .andExpect(status().isConflict());

        assertThat(professorRepository.count()).isEqualTo(1);
        assertThat(materialRepository.count()).isEqualTo(1);
    }

    @Test
    void deveImporUnicidadeSemDiferenciarCaixaNoBanco() {
        jdbcTemplate.update(
                "INSERT INTO professor (id, codigo, nome, competencia) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                "PROF-1",
                "Maria Silva",
                "Matemática");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO professor (id, codigo, nome, competencia) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                "prof-1",
                "Outra Pessoa",
                "História"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "INSERT INTO material (id, codigo, nome, tipo, quantidade) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                "MAT-1",
                "Projetor",
                "audiovisual",
                1);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO material (id, codigo, nome, tipo, quantidade) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                "mat-1",
                "Outro Projetor",
                "audiovisual",
                1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveBuscarProfessoresPorCompetenciaParcialSemDiferenciarCaixa() throws Exception {
        professorRepository.saveAll(List.of(
                new Professor("PROF-1", "Maria Silva", "Matemática Aplicada"),
                new Professor("PROF-2", "João Lima", "História")));

        mockMvc.perform(get("/api/v1/professores").param("competencia", "MATEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("PROF-1"));
    }

    @Test
    void deveBuscarMateriaisPorTipoParcialSemDiferenciarCaixa() throws Exception {
        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-1\",\"nome\":\"Projetor\","
                                + "\"tipo\":\"Audiovisual\",\"quantidade\":2}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MAT-2\",\"nome\":\"Mesa\","
                                + "\"tipo\":\"Mobiliário\",\"quantidade\":5}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/materiais").param("tipo", "AUDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("MAT-1"));
    }
}
