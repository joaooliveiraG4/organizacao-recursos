package br.com.senac.recursos.sala;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sala")
public class Sala {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false)
    private int capacidade;

    @Column(nullable = false, length = 120)
    private String localizacao;

    protected Sala() {
    }

    Sala(String codigo, String tipo, int capacidade, String localizacao) {
        this.id = UUID.randomUUID();
        this.codigo = codigo.trim().toUpperCase(Locale.ROOT);
        this.tipo = tipo.trim();
        this.capacidade = capacidade;
        this.localizacao = localizacao.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }
}
