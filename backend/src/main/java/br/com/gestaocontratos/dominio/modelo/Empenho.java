package br.com.gestaocontratos.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Nota de empenho do contrato. O saldo é calculado pelas OS que já consumiram o valor. */
@Entity
@Table(name = "empenhos")
public class Empenho extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private BigDecimal valor;

    private String descricao;

    protected Empenho() {
    }

    public Empenho(Contrato contrato, String numero, int ano, BigDecimal valor, String descricao) {
        this.contrato = contrato;
        this.numero = numero;
        this.ano = ano;
        this.valor = valor;
        this.descricao = descricao;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public String getNumero() {
        return numero;
    }

    public int getAno() {
        return ano;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }
}
