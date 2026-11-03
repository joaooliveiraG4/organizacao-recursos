package br.com.senac.recursos.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarMaterialRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 60) String tipo,
        @Positive int quantidade) {
}
