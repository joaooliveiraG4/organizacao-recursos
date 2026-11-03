package br.com.senac.recursos.material;

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
@RequestMapping("/api/v1/materiais")
class MaterialController {

    private final MaterialService materialService;

    MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    ResponseEntity<MaterialResponse> criar(@Valid @RequestBody CriarMaterialRequest request) {
        var material = materialService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(material.id())
                .toUri();
        return ResponseEntity.created(location).body(material);
    }

    @GetMapping
    List<MaterialResponse> listar() {
        return materialService.listar();
    }

    @GetMapping("/{id}")
    MaterialResponse consultar(@PathVariable UUID id) {
        return materialService.consultar(id);
    }
}
