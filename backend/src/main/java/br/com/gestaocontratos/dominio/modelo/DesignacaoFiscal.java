package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.Papel;
import br.com.gestaocontratos.dominio.TipoFiscal;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Designação de um servidor como fiscal do contrato, por portaria e com vigência. */
@Entity
@Table(name = "designacoes_fiscais")
public class DesignacaoFiscal extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String portaria;

    @Convert(converter = ConversorComValor.DeTipoFiscal.class)
    @Column(nullable = false)
    private TipoFiscal tipo;

    @Convert(converter = ConversorComValor.DePapel.class)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    protected DesignacaoFiscal() {
    }

    public DesignacaoFiscal(Contrato contrato, Usuario usuario, String portaria, TipoFiscal tipo, Papel papel,
                            LocalDate dataInicio, LocalDate dataFim) {
        this.contrato = contrato;
        this.usuario = usuario;
        this.portaria = portaria;
        this.tipo = tipo;
        this.papel = papel;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public boolean ativaEm(LocalDate data) {
        return !dataInicio.isAfter(data) && (dataFim == null || !dataFim.isBefore(data));
    }

    public void encerrarEm(LocalDate data) {
        this.dataFim = data;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getPortaria() {
        return portaria;
    }

    public TipoFiscal getTipo() {
        return tipo;
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
