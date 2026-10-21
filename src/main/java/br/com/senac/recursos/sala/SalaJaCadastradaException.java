package br.com.senac.recursos.sala;

class SalaJaCadastradaException extends RuntimeException {

    SalaJaCadastradaException(String codigo) {
        super("Já existe uma sala cadastrada com o código " + codigo + ".");
    }
}
