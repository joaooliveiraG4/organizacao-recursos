package br.com.senac.recursos.catalogo;

import java.util.UUID;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super(recurso + " não encontrado: " + id);
    }
}
