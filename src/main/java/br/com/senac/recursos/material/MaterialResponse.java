package br.com.senac.recursos.material;

import java.util.UUID;

public record MaterialResponse(UUID id, String codigo, String nome, String tipo, int quantidade) {

    static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getCodigo(),
                material.getNome(),
                material.getTipo(),
                material.getQuantidade());
    }
}
