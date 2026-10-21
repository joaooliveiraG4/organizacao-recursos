package br.com.senac.recursos.sala;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SalaService {

    private final SalaRepository salaRepository;

    SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @Transactional
    SalaResponse criar(CriarSalaRequest request) {
        var codigo = request.codigo().trim();
        if (salaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new SalaJaCadastradaException(codigo);
        }

        var sala = new Sala(
                codigo,
                request.tipo(),
                request.capacidade(),
                request.localizacao());
        return SalaResponse.from(salaRepository.saveAndFlush(sala));
    }

    @Transactional(readOnly = true)
    List<SalaResponse> listar() {
        return salaRepository.findAllByOrderByCodigoAsc().stream()
                .map(SalaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    SalaResponse consultar(UUID id) {
        return salaRepository.findById(id)
                .map(SalaResponse::from)
                .orElseThrow(() -> new SalaNaoEncontradaException(id));
    }
}
