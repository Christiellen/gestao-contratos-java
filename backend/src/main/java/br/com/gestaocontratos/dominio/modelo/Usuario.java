package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class Usuario extends EntidadeComDatas {

    @Column(name = "name", nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String senhaHash;

    @Convert(converter = ConversorComValor.DePerfil.class)
    @Column(nullable = false)
    private Perfil perfil;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public boolean isAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }

    public boolean isGestor() {
        return perfil == Perfil.GESTOR;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
