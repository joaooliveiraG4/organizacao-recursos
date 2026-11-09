package br.com.senac.recursos.sala;

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
@RequestMapping("/api/v1/salas")
class SalaController {

    private final SalaService salaService;

    SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @PostMapping
    ResponseEntity<SalaResponse> criar(@Valid @RequestBody CriarSalaRequest request) {
        var sala = salaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sala.id())
                .toUri();
        return ResponseEntity.created(location).body(sala);
    }

    @GetMapping
    List<SalaResponse> listar() {
        return salaService.listar();
    }

    @GetMapping("/{id}")
    SalaResponse consultar(@PathVariable UUID id) {
        return salaService.consultar(id);
    }
}
