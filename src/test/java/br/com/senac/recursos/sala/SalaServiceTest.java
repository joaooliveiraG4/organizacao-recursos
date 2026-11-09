package br.com.senac.recursos.sala;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalaServiceTest {

    @Mock
    private SalaRepository salaRepository;

    @InjectMocks
    private SalaService salaService;

    @Test
    void deveCriarSalaNormalizandoCodigo() {
        var request = new CriarSalaRequest(" b-101 ", "Aula", 30, " Bloco B ");
        when(salaRepository.existsByCodigoIgnoreCase("b-101")).thenReturn(false);
        when(salaRepository.saveAndFlush(any(Sala.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = salaService.criar(request);

        assertThat(response.codigo()).isEqualTo("B-101");
        assertThat(response.localizacao()).isEqualTo("Bloco B");
        verify(salaRepository).saveAndFlush(any(Sala.class));
    }

    @Test
    void deveRejeitarCodigoJaCadastradoSemSalvar() {
        var request = new CriarSalaRequest("B-101", "Aula", 30, "Bloco B");
        when(salaRepository.existsByCodigoIgnoreCase("B-101")).thenReturn(true);

        assertThatThrownBy(() -> salaService.criar(request))
                .isInstanceOf(SalaJaCadastradaException.class);
    }

    @Test
    void deveListarSalasEmOrdemDoRepositorio() {
        when(salaRepository.findAllByOrderByCodigoAsc())
                .thenReturn(List.of(new Sala("B-101", "Aula", 30, "Bloco B")));

        var responses = salaService.listar();

        assertThat(responses).extracting(SalaResponse::codigo).containsExactly("B-101");
    }

    @Test
    void deveConsultarSalaPorIdentificador() {
        var sala = new Sala("B-101", "Aula", 30, "Bloco B");
        when(salaRepository.findById(sala.getId())).thenReturn(Optional.of(sala));

        var response = salaService.consultar(sala.getId());

        assertThat(response.id()).isEqualTo(sala.getId());
    }

    @Test
    void deveInformarQuandoSalaNaoExiste() {
        var id = UUID.randomUUID();
        when(salaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salaService.consultar(id))
                .isInstanceOf(SalaNaoEncontradaException.class);
    }
}
