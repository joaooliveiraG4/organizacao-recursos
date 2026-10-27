package br.com.senac.recursos.seguranca;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
}
