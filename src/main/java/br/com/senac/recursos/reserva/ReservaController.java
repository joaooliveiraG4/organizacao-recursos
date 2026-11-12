package br.com.senac.recursos.reserva;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.senac.recursos.seguranca.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservas;
    private final RegistroConflitos conflitos;

    public ReservaController(ReservaService reservas, RegistroConflitos conflitos) {
        this.reservas = reservas;
        this.conflitos = conflitos;
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaCriada criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarReserva pedido) {
        var reserva = registrandoConflito(() -> reservas.criar(usuario.id(), pedido.inicio(), pedido.fim(),
                pedido.salaId(), pedido.professorId(), pedido.materialIds()));
        return new ReservaCriada(reserva.getId(), reserva.getEstado());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMINISTRADOR')")
    public ReservaCriada editar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id, @Valid @RequestBody CriarReserva pedido) {
        var reserva = registrandoConflito(() -> reservas.editar(usuario, id, pedido.inicio(), pedido.fim(),
                pedido.salaId(), pedido.professorId(), pedido.materialIds()));
        return new ReservaCriada(reserva.getId(), reserva.getEstado());
    }

    /**
     * RF-12 sem esgotar o pool (D-09): a transação da reserva já foi desfeita e
     * devolveu a conexão; só então o conflito é gravado, em transação própria.
     */
    private Reserva registrandoConflito(java.util.function.Supplier<Reserva> operacao) {
        try {
            return operacao.get();
        } catch (ConflitoReservaException conflito) {
            conflitos.registrar(conflito.tipoRecurso(), conflito.recursoId(), conflito.periodo());
            throw conflito;
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        reservas.cancelar(usuario, id);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaCriada aprovar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        var reserva = reservas.aprovar(usuario, id);
        return new ReservaCriada(reserva.getId(), reserva.getEstado());
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaCriada rejeitar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        var reserva = reservas.rejeitar(usuario, id);
        return new ReservaCriada(reserva.getId(), reserva.getEstado());
    }

    public record CriarReserva(@NotNull UUID salaId, UUID professorId,
            Set<@NotNull UUID> materialIds, @NotNull Instant inicio, @NotNull Instant fim) {
    }

    public record ReservaCriada(UUID id, EstadoReserva estado) {
    }
}
