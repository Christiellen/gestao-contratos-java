package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.TipoAditivo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aditivos")
public class Aditivo extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(nullable = false)
    private int numero;

    @Convert(converter = ConversorComValor.DeTipoAditivo.class)
    @Column(nullable = false)
    private TipoAditivo tipo;

    @Column(name = "data_assinatura", nullable = false)
    private LocalDate dataAssinatura;

    @Column(name = "data_fim_anterior")
    private LocalDate dataFimAnterior;

    @Column(name = "nova_data_fim")
    private LocalDate novaDataFim;

    @Column(name = "valor_acrescimo")
    private BigDecimal valorAcrescimo;

    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    protected Aditivo() {
    }

    public Aditivo(Contrato contrato, int numero, TipoAditivo tipo, LocalDate dataAssinatura, LocalDate dataFimAnterior,
                   LocalDate novaDataFim, BigDecimal valorAcrescimo, String descricao, Usuario registradoPor) {
        this.contrato = contrato;
        this.numero = numero;
        this.tipo = tipo;
        this.dataAssinatura = dataAssinatura;
        this.dataFimAnterior = dataFimAnterior;
        this.novaDataFim = novaDataFim;
        this.valorAcrescimo = valorAcrescimo;
        this.descricao = descricao;
        this.registradoPor = registradoPor;
    }

    public int getNumero() {
        return numero;
    }

    public TipoAditivo getTipo() {
        return tipo;
    }

    public LocalDate getDataAssinatura() {
        return dataAssinatura;
    }

    public LocalDate getDataFimAnterior() {
        return dataFimAnterior;
    }

    public LocalDate getNovaDataFim() {
        return novaDataFim;
    }

    public BigDecimal getValorAcrescimo() {
        return valorAcrescimo;
    }

    public String getDescricao() {
        return descricao;
    }
}
