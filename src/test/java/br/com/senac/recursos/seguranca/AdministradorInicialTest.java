package br.com.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdministradorInicialTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest(name = "email=''{0}'' senha=''{1}'' -> não cria")
    @CsvSource({
        "'',               ''",
        "admin@exemplo.test, ''",
        "'',               senha-local-123",
        "'   ',            senha-local-123",
    })
    void naoCriaAdministradorSemAsDuasVariaveis(String email, String senha) {
        new AdministradorInicial(repository, encoder, clock, email, senha).run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void naoDuplicaAdministradorJaExistente() {
        when(repository.existsByEmailIgnoreCase("admin@exemplo.test")).thenReturn(true);

        new AdministradorInicial(repository, encoder, clock, "admin@exemplo.test", "senha-local-123").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void criaAdministradorAtivoComSenhaCriptografada() {
        new AdministradorInicial(repository, encoder, clock, " Admin@Exemplo.test ", "senha-local-123").run(null);

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        var admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@exemplo.test");
        assertThat(admin.getRole()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(admin.isActive()).isTrue();
        assertThat(encoder.matches("senha-local-123", admin.getPasswordHash())).isTrue();
    }
}
