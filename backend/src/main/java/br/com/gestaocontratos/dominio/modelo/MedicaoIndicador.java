package br.com.gestaocontratos.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "medicoes_indicadores")
public class MedicaoIndicador extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicador_id")
    private Indicador indicador;

    @Column(nullable = false)
    private BigDecimal realizado;

    @Column(nullable = false)
    private BigDecimal previsto;

    @Column(nullable = false)
    private BigDecimal resultado;

    @Column(name = "percentual_glosa", nullable = false)
    private BigDecimal percentualGlosa;

    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    protected MedicaoIndicador() {
    }

    MedicaoIndicador(OrdemServico ordemServico, Indicador indicador, BigDecimal realizado, BigDecimal previsto,
                     BigDecimal resultado, BigDecimal percentualGlosa, String justificativa, Usuario registradoPor) {
        this.ordemServico = ordemServico;
        this.indicador = indicador;
        atualizar(realizado, previsto, resultado, percentualGlosa, justificativa, registradoPor);
    }

    final void atualizar(BigDecimal realizado, BigDecimal previsto, BigDecimal resultado, BigDecimal percentualGlosa,
                         String justificativa, Usuario registradoPor) {
        this.realizado = realizado;
        this.previsto = previsto;
        this.resultado = resultado;
        this.percentualGlosa = percentualGlosa;
        this.justificativa = justificativa;
        this.registradoPor = registradoPor;
    }

    public Indicador getIndicador() {
        return indicador;
    }

    public BigDecimal getRealizado() {
        return realizado;
    }

    public BigDecimal getPrevisto() {
        return previsto;
    }

    public BigDecimal getResultado() {
        return resultado;
    }

    public BigDecimal getPercentualGlosa() {
        return percentualGlosa;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
