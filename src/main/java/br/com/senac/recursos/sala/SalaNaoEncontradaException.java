package br.com.senac.recursos.sala;

import java.util.UUID;

class SalaNaoEncontradaException extends RuntimeException {

    SalaNaoEncontradaException(UUID id) {
        super("Sala não encontrada: " + id);
    }
}
