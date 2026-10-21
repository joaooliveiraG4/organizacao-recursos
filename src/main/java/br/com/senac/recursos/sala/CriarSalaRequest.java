package br.com.senac.recursos.sala;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarSalaRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 60) String tipo,
        @Positive int capacidade,
        @NotBlank @Size(max = 120) String localizacao) {
}
