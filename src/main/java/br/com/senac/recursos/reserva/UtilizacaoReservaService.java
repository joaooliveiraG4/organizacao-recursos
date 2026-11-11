package br.com.senac.recursos.reserva;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.senac.recursos.auditoria.AuditoriaService;
import br.com.senac.recursos.seguranca.UsuarioAutenticado;

/**
 * RF-09 — retirada e devolução (regras do João em 09/11/2026): Responsável ou
 * Administrador registra; APROVADA → EM_USO → CONCLUIDA; a devolução libera o
 * horário. A reserva é travada (PESSIMISTIC_WRITE) para que duas retiradas
 * simultâneas não sejam aceitas, e a auditoria entra na mesma transação.
 */
@Service
public class UtilizacaoReservaService {

    private final EntityManager entidades;
    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;

    public UtilizacaoReservaService(EntityManager entidades, JdbcTemplate jdbc, AuditoriaService auditoria) {
        this.entidades = entidades;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
    }

    @Transactional
    public Reserva registrarRetirada(UsuarioAutenticado usuario, UUID id) {
        var reserva = buscarComTrava(id);
        var anterior = reserva.getEstado();
        transicionar(reserva::registrarRetirada);
        registrar(reserva, anterior, "RETIRADA_REGISTRADA", usuario);
        return reserva;
    }

    @Transactional
    public Reserva registrarDevolucao(UsuarioAutenticado usuario, UUID id) {
        var reserva = buscarComTrava(id);
        var anterior = reserva.getEstado();
        transicionar(reserva::registrarDevolucao);
        // Estado final: os recursos deixam de bloquear o horário (regra do RF-04/06).
        jdbc.update("UPDATE reserva_recurso SET ativo = false WHERE reserva_id = ? AND ativo", id);
        registrar(reserva, anterior, "DEVOLUCAO_REGISTRADA", usuario);
        return reserva;
    }

    private Reserva buscarComTrava(UUID id) {
        var reserva = entidades.find(Reserva.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada.");
        }
        return reserva;
    }

    private static void transicionar(Runnable transicao) {
        try {
            transicao.run();
        } catch (TransicaoInvalidaException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserva não pode ser alterada nesse estado.");
        }
    }

    private void registrar(Reserva reserva, EstadoReserva anterior, String acao, UsuarioAutenticado usuario) {
        auditoria.registrar("RESERVA", reserva.getId(), acao, usuario.id(),
                Map.of("de", anterior.name(), "para", reserva.getEstado().name()));
    }
}
