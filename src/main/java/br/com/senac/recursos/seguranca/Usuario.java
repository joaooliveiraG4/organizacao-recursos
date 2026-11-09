package br.com.senac.recursos.seguranca;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Usuario() {
    }

    public Usuario(String name, String email, Perfil role, String passwordHash, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.name = name.trim();
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.role = role;
        this.active = true;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public void desativar() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Perfil getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
