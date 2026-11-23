package br.com.gestaocontratos.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Contrato administrativo. A exclusão é lógica (deleted_at), para preservar o histórico. */
@Entity
@Table(name = "contratos")
@SQLDelete(sql = "UPDATE contratos SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Contrato extends EntidadeComDatas {

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private String objeto;

    @Column(nullable = false)
    private String contratada;

    @Column(nullable = false)
    private String cnpj;

    @Column(name = "valor_global", nullable = false)
    private BigDecimal valorGlobal;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id")
    private Usuario gestor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_id")
    private Usuario fiscal;

    @Column(name = "deleted_at")
    private Instant excluidoEm;

    @OneToMany(mappedBy = "contrato")
    @OrderBy("numero")
    private List<Aditivo> aditivos = new ArrayList<>();

    @OneToMany(mappedBy = "contrato")
    @OrderBy("id")
    private List<DesignacaoFiscal> designacoes = new ArrayList<>();

    @OneToMany(mappedBy = "contrato")
    @OrderBy("id")
    private List<Preposto> prepostos = new ArrayList<>();

    @OneToMany(mappedBy = "contrato")
    @OrderBy("ano, numero")
    private List<Empenho> empenhos = new ArrayList<>();

    @OneToMany(mappedBy = "contrato")
    @OrderBy("sigla")
    private List<Indicador> indicadores = new ArrayList<>();

    protected Contrato() {
    }

    public Contrato(String numero, String objeto, String contratada, String cnpj, BigDecimal valorGlobal,
                    LocalDate dataInicio, LocalDate dataFim, Usuario gestor, Usuario fiscal) {
        alterar(numero, objeto, contratada, cnpj, valorGlobal, dataInicio, dataFim, gestor, fiscal);
    }

    public final void alterar(String numero, String objeto, String contratada, String cnpj, BigDecimal valorGlobal,
                              LocalDate dataInicio, LocalDate dataFim, Usuario gestor, Usuario fiscal) {
        this.numero = numero;
        this.objeto = objeto;
        this.contratada = contratada;
        this.cnpj = cnpj;
        this.valorGlobal = valorGlobal;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.gestor = gestor;
        this.fiscal = fiscal;
    }

    /** Aplica um aditivo de prazo. */
    public void prorrogarAte(LocalDate novaDataFim) {
        this.dataFim = novaDataFim;
    }

    /** Aplica um aditivo de valor (positivo ou negativo). */
    public void somarAoValorGlobal(BigDecimal acrescimo) {
        this.valorGlobal = valorGlobal.add(acrescimo);
    }

    public boolean geridoPor(Usuario usuario) {
        return gestor.getId().equals(usuario.getId());
    }

    public String getNumero() {
        return numero;
    }

    public String getObjeto() {
        return objeto;
    }

    public String getContratada() {
        return contratada;
    }

    public String getCnpj() {
        return cnpj;
    }

    public BigDecimal getValorGlobal() {
        return valorGlobal;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public Usuario getFiscal() {
        return fiscal;
    }

    public List<Aditivo> getAditivos() {
        return aditivos;
    }

    public List<DesignacaoFiscal> getDesignacoes() {
        return designacoes;
    }

    public List<Preposto> getPrepostos() {
        return prepostos;
    }

    public List<Empenho> getEmpenhos() {
        return empenhos;
    }

    public List<Indicador> getIndicadores() {
        return indicadores;
    }
}
