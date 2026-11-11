package br.com.senac.recursos.professor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.senac.recursos.catalogo.RecursoJaCadastradoException;
import br.com.senac.recursos.catalogo.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private ProfessorService professorService;

    @Test
    void deveCriarProfessorNormalizandoCodigo() {
        when(professorRepository.existsByCodigoIgnoreCase("prof-1")).thenReturn(false);
        when(professorRepository.saveAndFlush(any(Professor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = professorService.criar(
                new CriarProfessorRequest(" prof-1 ", " Maria Silva ", " Matemática "));

        assertThat(response.codigo()).isEqualTo("PROF-1");
        assertThat(response.nome()).isEqualTo("Maria Silva");
        assertThat(response.competencia()).isEqualTo("Matemática");
        verify(professorRepository).saveAndFlush(any(Professor.class));
    }

    @Test
    void deveRejeitarCodigoDuplicadoSemSalvar() {
        when(professorRepository.existsByCodigoIgnoreCase("PROF-1")).thenReturn(true);

        assertThatThrownBy(() -> professorService.criar(
                new CriarProfessorRequest("PROF-1", "Maria Silva", "Matemática")))
                .isInstanceOf(RecursoJaCadastradoException.class);
    }

    @Test
    void deveBuscarProfessoresPorCompetencia() {
        when(professorRepository.buscarPorCompetencia("matematica"))
                .thenReturn(List.of(new Professor("PROF-1", "Maria Silva", "Matemática")));

        assertThat(professorService.buscar(" matematica "))
                .extracting(ProfessorResponse::codigo)
                .containsExactly("PROF-1");
    }

    @Test
    void deveIgnorarCompetenciaEmBranco() {
        when(professorRepository.buscarPorCompetencia(null)).thenReturn(List.of());

        assertThat(professorService.buscar(" ")).isEmpty();
    }

    @Test
    void deveConsultarProfessorPorId() {
        var professor = new Professor("PROF-1", "Maria Silva", "Matemática");
        when(professorRepository.findById(professor.getId())).thenReturn(Optional.of(professor));

        assertThat(professorService.consultar(professor.getId()).id()).isEqualTo(professor.getId());
    }

    @Test
    void deveInformarQuandoProfessorNaoExiste() {
        var id = UUID.randomUUID();
        when(professorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.consultar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
