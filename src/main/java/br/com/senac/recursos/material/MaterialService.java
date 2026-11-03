package br.com.senac.recursos.material;

import java.util.List;
import java.util.UUID;

import br.com.senac.recursos.catalogo.RecursoJaCadastradoException;
import br.com.senac.recursos.catalogo.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MaterialService {

    private final MaterialRepository materialRepository;

    MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @Transactional
    MaterialResponse criar(CriarMaterialRequest request) {
        var codigo = request.codigo().trim();
        if (materialRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new RecursoJaCadastradoException("material", codigo);
        }

        var material = new Material(
                codigo,
                request.nome(),
                request.tipo(),
                request.quantidade());
        return MaterialResponse.from(materialRepository.saveAndFlush(material));
    }

    @Transactional(readOnly = true)
    List<MaterialResponse> listar() {
        return materialRepository.findAllByOrderByCodigoAsc().stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    MaterialResponse consultar(UUID id) {
        return materialRepository.findById(id)
                .map(MaterialResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", id));
    }
}
