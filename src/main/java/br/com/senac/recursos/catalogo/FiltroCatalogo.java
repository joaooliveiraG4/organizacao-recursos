package br.com.senac.recursos.catalogo;

public final class FiltroCatalogo {

    private FiltroCatalogo() {
    }

    public static String normalizarTexto(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return null;
        }
        return filtro.trim();
    }
}
