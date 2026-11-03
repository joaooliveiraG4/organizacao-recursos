package br.com.senac.recursos.professor;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "professor")
public class Professor {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 120)
    private String competencia;

    protected Professor() {
    }

    Professor(String codigo, String nome, String competencia) {
        this.id = UUID.randomUUID();
        this.codigo = codigo.trim().toUpperCase(Locale.ROOT);
        this.nome = nome.trim();
        this.competencia = competencia.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCompetencia() {
        return competencia;
    }
}
