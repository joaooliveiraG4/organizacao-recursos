package br.com.senac.recursos.seguranca;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria um Administrador para uso local somente quando as variáveis
 * BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD estão definidas.
 * Nenhuma credencial fica versionada no repositório.
 */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String senha;

    AdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${bootstrap.admin.email:}") String email,
            @Value("${bootstrap.admin.password:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank() || usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }
        usuarioRepository.save(new Usuario(
                "Administrador inicial",
                email,
                Perfil.ADMINISTRADOR,
                passwordEncoder.encode(senha),
                Instant.now(clock)));
        LOG.info("Administrador inicial criado a partir das variáveis de ambiente.");
    }
}
