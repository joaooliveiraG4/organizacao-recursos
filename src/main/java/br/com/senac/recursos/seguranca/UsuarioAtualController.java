package br.com.senac.recursos.seguranca;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class UsuarioAtualController {

    @GetMapping
    UsuarioAtualResponse atual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new UsuarioAtualResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.perfil());
    }

    record UsuarioAtualResponse(UUID id, String nome, String email, Perfil perfil) {
    }
}
