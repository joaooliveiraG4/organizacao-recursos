package br.com.senac.recursos.catalogo;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.senac.recursos.auditoria.AuditoriaService;
import br.com.senac.recursos.seguranca.UsuarioAutenticado;

/**
 * RF-08 — Administrador coloca ou tira sala/material de manutenção. Enquanto
 * marcado, o recurso não entra em reserva nova nem editada (409). A mudança é
 * auditada na mesma transação.
 */
@RestController
public class ManutencaoController {

    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;

    public ManutencaoController(JdbcTemplate jdbc, AuditoriaService auditoria) {
        this.jdbc = jdbc;
        this.auditoria = auditoria;
    }

    public record PedidoManutencao(@NotNull Boolean emManutencao) {
    }

    public record EstadoManutencao(UUID id, boolean emManutencao) {
    }

    @PutMapping("/api/v1/salas/{id}/manutencao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public EstadoManutencao sala(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id, @Valid @RequestBody PedidoManutencao pedido) {
        return marcar(SQL_SALA, "SALA", usuario, id, pedido.emManutencao());
    }

    @PutMapping("/api/v1/materiais/{id}/manutencao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public EstadoManutencao material(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id, @Valid @RequestBody PedidoManutencao pedido) {
        return marcar(SQL_MATERIAL, "MATERIAL", usuario, id, pedido.emManutencao());
    }

    // Consultas fixas por tipo: nenhum SQL é montado por concatenação (Sonar java:S2077).
    private static final String SQL_SALA = "UPDATE sala SET em_manutencao = ? WHERE id = ?";
    private static final String SQL_MATERIAL = "UPDATE material SET em_manutencao = ? WHERE id = ?";

    private EstadoManutencao marcar(String sql, String tipo, UsuarioAutenticado usuario, UUID id,
            boolean emManutencao) {
        int alteradas = jdbc.update(sql, emManutencao, id);
        if (alteradas == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
        }
        auditoria.registrar(tipo, id, emManutencao ? "MANUTENCAO_INICIADA" : "MANUTENCAO_ENCERRADA",
                usuario.id(), Map.of("emManutencao", String.valueOf(emManutencao)));
        return new EstadoManutencao(id, emManutencao);
    }
}
