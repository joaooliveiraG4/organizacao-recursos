package br.com.senac.recursos.professor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/professores")
class ProfessorController {

    private final ProfessorService professorService;

    ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @PostMapping
    ResponseEntity<ProfessorResponse> criar(@Valid @RequestBody CriarProfessorRequest request) {
        var professor = professorService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(professor.id())
                .toUri();
        return ResponseEntity.created(location).body(professor);
    }

    @GetMapping
    List<ProfessorResponse> listar() {
        return professorService.listar();
    }

    @GetMapping("/{id}")
    ProfessorResponse consultar(@PathVariable UUID id) {
        return professorService.consultar(id);
    }
}
