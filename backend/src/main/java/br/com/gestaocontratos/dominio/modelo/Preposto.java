package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.Papel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Representante da empresa contratada. */
@Entity
@Table(name = "prepostos")
public class Preposto extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    private String telefone;

    @Convert(converter = ConversorComValor.DePapel.class)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    protected Preposto() {
    }

    public Preposto(Contrato contrato, String nome, String email, String telefone, Papel papel, LocalDate dataInicio, LocalDate dataFim) {
        this.contrato = contrato;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.papel = papel;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public boolean ativoEm(LocalDate data) {
        return !dataInicio.isAfter(data) && (dataFim == null || !dataFim.isBefore(data));
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public Papel getPapel() {
        return papel;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }
}
