package br.com.senac.recursos.seguranca;

public enum Perfil {
    SOLICITANTE,
    RESPONSAVEL,
    ADMINISTRADOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
