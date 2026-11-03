package br.com.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UsuarioDetailsServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(repository);

    @Test
    void converteUsuarioEmPrincipalComAuthorityDoPerfil() {
        var usuario = new Usuario("Rita", "rita@exemplo.test", Perfil.RESPONSAVEL, "hash", Instant.EPOCH);
        when(repository.findByEmailIgnoreCase("rita@exemplo.test")).thenReturn(Optional.of(usuario));

        var principal = service.loadUserByUsername(" rita@exemplo.test ");

        assertThat(principal.getUsername()).isEqualTo("rita@exemplo.test");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RESPONSAVEL");
    }

    @Test
    void usuarioDesativadoFicaDesabilitado() {
        var usuario = new Usuario("Rita", "rita@exemplo.test", Perfil.RESPONSAVEL, "hash", Instant.EPOCH);
        usuario.desativar();
        when(repository.findByEmailIgnoreCase("rita@exemplo.test")).thenReturn(Optional.of(usuario));

        assertThat(service.loadUserByUsername("rita@exemplo.test").isEnabled()).isFalse();
    }

    @Test
    void usuarioInexistenteGeraErroComMensagemNeutra() {
        when(repository.findByEmailIgnoreCase("x@exemplo.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@exemplo.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciais inválidas.");
    }
}
