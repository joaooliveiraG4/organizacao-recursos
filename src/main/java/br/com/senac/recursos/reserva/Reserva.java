package br.com.senac.recursos.reserva;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    private UUID id;

    @Column(name = "solicitante_id", nullable = false, updatable = false)
    private UUID solicitanteId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoReserva estado;

    protected Reserva() {
    }

    public Reserva(UUID solicitanteId, PeriodoReserva periodo) {
        this.id = UUID.randomUUID();
        this.solicitanteId = Objects.requireNonNull(solicitanteId, "Informe o solicitante");
        this.inicio = periodo.inicio();
        this.fim = periodo.fim();
        this.estado = EstadoReserva.SOLICITADA;
    }

    void editar(PeriodoReserva periodo, boolean restrita) {
        inicio = periodo.inicio();
        fim = periodo.fim();
        if (restrita) {
            estado = EstadoReserva.SOLICITADA;
        }
    }

    void cancelar() {
        estado = EstadoReserva.CANCELADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSolicitanteId() {
        return solicitanteId;
    }

    public PeriodoReserva getPeriodo() {
        return new PeriodoReserva(inicio, fim);
    }

    public EstadoReserva getEstado() {
        return estado;
    }
}
