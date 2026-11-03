package br.com.senac.recursos.professor;

import java.util.List;
import java.util.UUID;

import br.com.senac.recursos.catalogo.RecursoJaCadastradoException;
import br.com.senac.recursos.catalogo.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProfessorService {

    private final ProfessorRepository professorRepository;

    ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    @Transactional
    ProfessorResponse criar(CriarProfessorRequest request) {
        var codigo = request.codigo().trim();
        if (professorRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new RecursoJaCadastradoException("professor", codigo);
        }

        var professor = new Professor(codigo, request.nome(), request.competencia());
        return ProfessorResponse.from(professorRepository.saveAndFlush(professor));
    }

    @Transactional(readOnly = true)
    List<ProfessorResponse> listar() {
        return professorRepository.findAllByOrderByCodigoAsc().stream()
                .map(ProfessorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    ProfessorResponse consultar(UUID id) {
        return professorRepository.findById(id)
                .map(ProfessorResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor", id));
    }
}
