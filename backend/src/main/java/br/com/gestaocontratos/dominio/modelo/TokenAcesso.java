package br.com.gestaocontratos.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Token de acesso à API (Bearer). Guarda só o hash; o texto do token sai uma única vez, no login. */
@Entity
@Table(name = "tokens_acesso")
public class TokenAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "created_at", nullable = false)
    private Instant criadoEm;

    protected TokenAcesso() {
    }

    public TokenAcesso(Usuario usuario, String hash, Instant criadoEm, Instant expiraEm) {
        this.usuario = usuario;
        this.hash = hash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean expiradoEm(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getHash() {
        return hash;
    }
}
