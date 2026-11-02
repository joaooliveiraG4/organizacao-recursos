package br.com.senac.recursos.catalogo;

public class RecursoJaCadastradoException extends RuntimeException {

    public RecursoJaCadastradoException(String recurso, String codigo) {
        super("Já existe um " + recurso + " cadastrado com o código " + codigo + ".");
    }
}
