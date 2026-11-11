package br.com.senac.recursos.seguranca;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal da sessão autenticada. Expõe o id para que auditoria e reservas
 * registrem o autor real da operação.
 */
public record UsuarioAutenticado(
        UUID id,
        String nome,
        String email,
        Perfil perfil,
        String senhaHash,
        boolean ativo) implements UserDetails {

    static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getName(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getPasswordHash(),
                usuario.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil.authority()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
