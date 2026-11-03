package br.com.senac.recursos.professor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarProfessorRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 120) String competencia) {
}
