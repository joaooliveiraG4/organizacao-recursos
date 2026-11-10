package br.com.senac.recursos.material;

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
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void deveCriarMaterialNormalizandoCodigo() {
        when(materialRepository.existsByCodigoIgnoreCase("mat-1")).thenReturn(false);
        when(materialRepository.saveAndFlush(any(Material.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = materialService.criar(
                new CriarMaterialRequest(" mat-1 ", " Projetor ", " audiovisual ", 2));

        assertThat(response.codigo()).isEqualTo("MAT-1");
        assertThat(response.nome()).isEqualTo("Projetor");
        assertThat(response.tipo()).isEqualTo("audiovisual");
        assertThat(response.quantidade()).isEqualTo(2);
        verify(materialRepository).saveAndFlush(any(Material.class));
    }

    @Test
    void deveRejeitarCodigoDuplicadoSemSalvar() {
        when(materialRepository.existsByCodigoIgnoreCase("MAT-1")).thenReturn(true);

        assertThatThrownBy(() -> materialService.criar(
                new CriarMaterialRequest("MAT-1", "Projetor", "audiovisual", 2)))
                .isInstanceOf(RecursoJaCadastradoException.class);
    }

    @Test
    void deveBuscarMateriaisPorTipo() {
        when(materialRepository.buscarPorTipo("audiovisual"))
                .thenReturn(List.of(new Material("MAT-1", "Projetor", "audiovisual", 2)));

        assertThat(materialService.buscar(" audiovisual "))
                .extracting(MaterialResponse::codigo)
                .containsExactly("MAT-1");
    }

    @Test
    void deveIgnorarTipoEmBranco() {
        when(materialRepository.buscarPorTipo(null)).thenReturn(List.of());

        assertThat(materialService.buscar(" ")).isEmpty();
    }

    @Test
    void deveConsultarMaterialPorId() {
        var material = new Material("MAT-1", "Projetor", "audiovisual", 2);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThat(materialService.consultar(material.getId()).id()).isEqualTo(material.getId());
    }

    @Test
    void deveInformarQuandoMaterialNaoExiste() {
        var id = UUID.randomUUID();
        when(materialRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.consultar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
