package br.com.senac.recursos.reserva;

/** A reserva não está no estado que a operação exige. */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(EstadoReserva atual, EstadoReserva exigido) {
        super("A reserva está " + atual + " e a operação exige " + exigido + ".");
    }
}
