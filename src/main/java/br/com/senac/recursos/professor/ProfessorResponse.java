package br.com.senac.recursos.professor;

import java.util.UUID;

public record ProfessorResponse(UUID id, String codigo, String nome, String competencia) {

    static ProfessorResponse from(Professor professor) {
        return new ProfessorResponse(
                professor.getId(),
                professor.getCodigo(),
                professor.getNome(),
                professor.getCompetencia());
    }
}
