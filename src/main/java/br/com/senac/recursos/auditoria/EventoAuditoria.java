package br.com.senac.recursos.auditoria;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "audit_event")
class EventoAuditoria {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String tipo;

    @Column(name = "aggregate_id", nullable = false)
    private UUID registroId;

    @Column(name = "action", nullable = false, length = 80)
    private String acao;

    @Column(name = "actor_id", nullable = false)
    private UUID autorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant ocorridoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false)
    private Map<String, Object> detalhes;

    @Column(name = "sequence_number", insertable = false, updatable = false)
    private Long sequencia;

    protected EventoAuditoria() {
    }

    EventoAuditoria(
            String tipo,
            UUID registroId,
            String acao,
            UUID autorId,
            Instant ocorridoEm,
            Map<String, Object> detalhes) {
        this.id = UUID.randomUUID();
        this.tipo = tipo;
        this.registroId = registroId;
        this.acao = acao;
        this.autorId = autorId;
        this.ocorridoEm = ocorridoEm;
        this.detalhes = detalhes;
    }

    RegistroAuditoria paraRegistro() {
        return new RegistroAuditoria(id, tipo, registroId, acao, autorId, ocorridoEm, detalhes);
    }
}
