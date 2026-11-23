package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.Faixa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Indicador de nível de serviço. O resultado é 100 * realizado / previsto.
 * As faixas (JSONB) definem a glosa. Ex.: [{"minimo":95,"glosa":0},{"minimo":90,"glosa":2}].
 */
@Entity
@Table(name = "indicadores")
public class Indicador extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(nullable = false)
    private String sigla;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private BigDecimal meta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Faixa> faixas;

    protected Indicador() {
    }

    public Indicador(Contrato contrato, String sigla, String nome, String descricao, BigDecimal meta, List<Faixa> faixas) {
        this.contrato = contrato;
        this.sigla = sigla;
        this.nome = nome;
        this.descricao = descricao;
        this.meta = meta;
        this.faixas = faixas;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getMeta() {
        return meta;
    }

    public List<Faixa> getFaixas() {
        return faixas;
    }
}
