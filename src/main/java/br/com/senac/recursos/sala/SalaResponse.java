package br.com.senac.recursos.sala;

import java.util.UUID;

public record SalaResponse(
        UUID id,
        String codigo,
        String tipo,
        int capacidade,
        String localizacao) {

    static SalaResponse from(Sala sala) {
        return new SalaResponse(
                sala.getId(),
                sala.getCodigo(),
                sala.getTipo(),
                sala.getCapacidade(),
                sala.getLocalizacao());
    }
}
