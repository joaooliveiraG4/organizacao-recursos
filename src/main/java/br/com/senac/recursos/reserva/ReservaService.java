package br.com.senac.recursos.reserva;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.context.ApplicationEventPublisher;

import br.com.senac.recursos.auditoria.AuditoriaService;
import br.com.senac.recursos.notificacao.Notificacao;
import br.com.senac.recursos.notificacao.TipoNotificacao;
import br.com.senac.recursos.seguranca.Perfil;
import br.com.senac.recursos.seguranca.UsuarioAutenticado;

@Service
public class ReservaService {

    private final EntityManager entidades;
    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public ReservaService(EntityManager entidades, JdbcTemplate jdbc, AuditoriaService auditoria,
            ApplicationEventPublisher eventos) {
        this.entidades = entidades;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Transactional
    public Reserva criar(UUID solicitante, Instant inicio, Instant fim, UUID sala, UUID professor,
            Set<UUID> materiais) {
        var periodo = validarPeriodo(inicio, fim);
        var recursos = recursos(sala, professor, materiais);
        var restrita = bloquearRecursos(recursos);
        exigirForaDeManutencao(recursos);
        var reserva = new Reserva(solicitante, periodo);
        if (!restrita) {
            reserva.aprovar();
        }
        entidades.persist(reserva);
        entidades.flush();

        for (var recurso : recursos) {
            alocar(reserva, recurso);
        }
        auditoria.registrar("RESERVA", reserva.getId(), "RESERVA_CRIADA", solicitante,
                Map.of("para", reserva.getEstado().name()));
        if (!restrita) {
            notificar(reserva, TipoNotificacao.RESERVA_APROVADA);
        }
        return reserva;
    }

    @Transactional
    public Reserva editar(UsuarioAutenticado usuario, UUID id, Instant inicio, Instant fim,
            UUID sala, UUID professor, Set<UUID> materiais) {
        var reserva = buscarEditavel(usuario, id);
        var periodo = validarPeriodo(inicio, fim);
        var novos = recursos(sala, professor, materiais);
        var envolvidos = new HashSet<>(novos);
        envolvidos.addAll(jdbc.query("""
                SELECT tipo_recurso, recurso_id FROM reserva_recurso WHERE reserva_id = ?
                """, (linha, numero) -> new Recurso(Tipo.valueOf(linha.getString("tipo_recurso")),
                        linha.getObject("recurso_id", UUID.class)), id));

        // Bloqueia os recursos antigos e novos antes de trocar as alocações.
        var restrita = bloquearRecursos(envolvidos);
        exigirForaDeManutencao(novos);
        var anterior = reserva.getEstado();
        reserva.editar(periodo, restrita);
        jdbc.update("DELETE FROM reserva_recurso WHERE reserva_id = ?", id);
        for (var recurso : novos) {
            alocar(reserva, recurso);
        }
        registrarMudanca(reserva, anterior, "RESERVA_EDITADA", usuario.id());
        return reserva;
    }

    @Transactional
    public void cancelar(UsuarioAutenticado usuario, UUID id) {
        var reserva = buscarEditavel(usuario, id);
        var anterior = reserva.getEstado();
        reserva.cancelar();
        jdbc.update("UPDATE reserva_recurso SET ativo = false WHERE reserva_id = ? AND ativo", id);
        registrarMudanca(reserva, anterior, "RESERVA_CANCELADA", usuario.id());
    }

    @Transactional
    public Reserva aprovar(UsuarioAutenticado usuario, UUID id) {
        var reserva = buscarParaDecisao(id);
        var anterior = reserva.getEstado();
        try {
            reserva.aprovar();
        } catch (TransicaoInvalidaException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reserva não pode ser aprovada nesse estado.");
        }
        registrarMudanca(reserva, anterior, "RESERVA_APROVADA", usuario.id());
        notificar(reserva, TipoNotificacao.RESERVA_APROVADA);
        return reserva;
    }

    /** RF-07: Responsável recusa a reserva pendente; o horário é liberado. */
    @Transactional
    public Reserva rejeitar(UsuarioAutenticado usuario, UUID id) {
        var reserva = buscarParaDecisao(id);
        var anterior = reserva.getEstado();
        try {
            reserva.rejeitar();
        } catch (TransicaoInvalidaException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reserva não pode ser rejeitada nesse estado.");
        }
        jdbc.update("UPDATE reserva_recurso SET ativo = false WHERE reserva_id = ? AND ativo", id);
        registrarMudanca(reserva, anterior, "RESERVA_REJEITADA", usuario.id());
        notificar(reserva, TipoNotificacao.RESERVA_REJEITADA);
        return reserva;
    }

    private Reserva buscarParaDecisao(UUID id) {
        var reserva = entidades.find(Reserva.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada.");
        }
        return reserva;
    }

    /** RF-11: publicado dentro da transação; o envio acontece só após o commit. */
    private void notificar(Reserva reserva, TipoNotificacao tipo) {
        eventos.publishEvent(new Notificacao(reserva.getSolicitanteId(), tipo, reserva.getId()));
    }

    private Reserva buscarEditavel(UsuarioAutenticado usuario, UUID id) {
        var reserva = entidades.find(Reserva.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada.");
        }
        if (usuario.perfil() != Perfil.ADMINISTRADOR && !reserva.getSolicitanteId().equals(usuario.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode alterar esta reserva.");
        }
        if (reserva.getEstado() != EstadoReserva.SOLICITADA && reserva.getEstado() != EstadoReserva.APROVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserva não pode ser alterada nesse estado.");
        }
        return reserva;
    }

    private void registrarMudanca(Reserva reserva, EstadoReserva anterior, String acao, UUID autor) {
        auditoria.registrar("RESERVA", reserva.getId(), acao, autor,
                Map.of("de", anterior.name(), "para", reserva.getEstado().name()));
    }

    private static PeriodoReserva validarPeriodo(Instant inicio, Instant fim) {
        try {
            return new PeriodoReserva(inicio, fim);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static List<Recurso> recursos(UUID sala, UUID professor, Set<UUID> materiais) {
        var recursos = new ArrayList<Recurso>();
        recursos.add(new Recurso(Tipo.SALA, sala));
        if (professor != null) {
            recursos.add(new Recurso(Tipo.PROFESSOR, professor));
        }
        if (materiais != null) {
            materiais.stream().sorted().map(id -> new Recurso(Tipo.MATERIAL, id)).forEach(recursos::add);
        }
        return recursos;
    }

    /**
     * RF-08: recurso em manutenção não pode entrar em reserva nova nem editada.
     * Roda depois do FOR NO KEY UPDATE, então a marcação não muda no meio da operação.
     */
    private void exigirForaDeManutencao(Iterable<Recurso> recursos) {
        for (var recurso : recursos) {
            Boolean emManutencao = jdbc.queryForObject(recurso.tipo().sqlManutencao, Boolean.class, recurso.id());
            if (Boolean.TRUE.equals(emManutencao)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Recurso em manutenção.");
            }
        }
    }

    private boolean bloquearRecursos(Iterable<Recurso> recursos) {
        var ordenados = new ArrayList<Recurso>();
        recursos.forEach(ordenados::add);
        ordenados.sort(Comparator.comparing(Recurso::tipo).thenComparing(Recurso::id));
        boolean restrito = false;
        for (var recurso : ordenados) {
            var encontrados = jdbc.queryForList(recurso.tipo().sqlTravar, Boolean.class, recurso.id());
            if (encontrados.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
            }
            restrito |= encontrados.getFirst();
        }
        return restrito;
    }

    private void alocar(Reserva reserva, Recurso recurso) {
        var periodo = reserva.getPeriodo();
        try {
            jdbc.update("""
                    INSERT INTO reserva_recurso (reserva_id, tipo_recurso, recurso_id, periodo)
                    VALUES (?, ?, ?, tstzrange(?, ?, '[)'))
                    """, reserva.getId(), recurso.tipo().name(), recurso.id(), periodo.inicio().atOffset(ZoneOffset.UTC),
                    periodo.fim().atOffset(ZoneOffset.UTC));
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException sql && "23P01".equals(sql.getSQLState())) {
                // O conflito é gravado pelo controller, fora desta transação (D-09).
                throw new ConflitoReservaException(recurso.tipo().name(), recurso.id(), periodo);
            }
            throw e;
        }
    }

    /**
     * Cada tipo carrega suas consultas completas e fixas: nenhum SQL é montado
     * por concatenação (Sonar java:S2077). Os valores sempre vão por parâmetro.
     */
    private enum Tipo {
        SALA("SELECT restrito FROM sala WHERE id = ? FOR NO KEY UPDATE",
                "SELECT em_manutencao FROM sala WHERE id = ?"),
        PROFESSOR("SELECT restrito FROM professor WHERE id = ? FOR NO KEY UPDATE",
                "SELECT em_manutencao FROM professor WHERE id = ?"),
        MATERIAL("SELECT restrito FROM material WHERE id = ? FOR NO KEY UPDATE",
                "SELECT em_manutencao FROM material WHERE id = ?");

        private final String sqlTravar;
        private final String sqlManutencao;

        Tipo(String sqlTravar, String sqlManutencao) {
            this.sqlTravar = sqlTravar;
            this.sqlManutencao = sqlManutencao;
        }
    }

    private record Recurso(Tipo tipo, UUID id) {
    }
}
