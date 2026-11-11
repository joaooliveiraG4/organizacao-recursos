package br.com.senac.recursos.reserva;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senac.recursos.seguranca.UsuarioAutenticado;

/** RF-09 — registro de retirada e devolução. Solicitante recebe 403. */
@RestController
@RequestMapping("/api/v1/reservas/{id}")
public class UtilizacaoReservaController {

    private final UtilizacaoReservaService utilizacao;

    public UtilizacaoReservaController(UtilizacaoReservaService utilizacao) {
        this.utilizacao = utilizacao;
    }

    @PostMapping("/retirada")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservaController.ReservaCriada retirar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id) {
        var reserva = utilizacao.registrarRetirada(usuario, id);
        return new ReservaController.ReservaCriada(reserva.getId(), reserva.getEstado());
    }

    @PostMapping("/devolucao")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservaController.ReservaCriada devolver(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id) {
        var reserva = utilizacao.registrarDevolucao(usuario, id);
        return new ReservaController.ReservaCriada(reserva.getId(), reserva.getEstado());
    }
}
