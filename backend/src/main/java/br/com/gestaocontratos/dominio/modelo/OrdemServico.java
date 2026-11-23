package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.CalculadoraGlosa.ValoresGlosa;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.TipoOrdemServico;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(name = "ordens_servico")
public class OrdemServico extends EntidadeComDatas {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empenho_id")
    private Empenho empenho;

    @Column(nullable = false)
    private String numero;

    @Convert(converter = ConversorComValor.DeTipoOrdemServico.class)
    @Column(nullable = false)
    private TipoOrdemServico tipo;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_estimado", nullable = false)
    private BigDecimal valorEstimado;

    @Column(name = "percentual_glosa", nullable = false)
    private BigDecimal percentualGlosa = BigDecimal.ZERO;

    @Column(name = "valor_glosa", nullable = false)
    private BigDecimal valorGlosa = BigDecimal.ZERO;

    @Column(name = "valor_final")
    private BigDecimal valorFinal;

    @Convert(converter = ConversorComValor.DeFase.class)
    @Column(nullable = false)
    private FaseOrdemServico fase;

    @Column(name = "prazo_entrega", nullable = false)
    private LocalDate prazoEntrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por")
    private Usuario criadoPor;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<MedicaoIndicador> medicoes = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.PERSIST)
    @OrderBy("ocorridoEm, id")
    private List<EventoOrdemServico> eventos = new ArrayList<>();

    protected OrdemServico() {
    }

    public OrdemServico(Contrato contrato, Empenho empenho, String numero, TipoOrdemServico tipo, String descricao,
                        BigDecimal valorEstimado, LocalDate prazoEntrega, Usuario criadoPor) {
        this.contrato = contrato;
        this.empenho = empenho;
        this.numero = numero;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valorEstimado = valorEstimado;
        this.prazoEntrega = prazoEntrega;
        this.criadoPor = criadoPor;
        this.fase = FaseOrdemServico.EM_PREPARACAO;
    }

    /** Inclui ou substitui a medição de um indicador. */
    public void registrarMedicao(Indicador indicador, BigDecimal realizado, BigDecimal previsto, BigDecimal resultado,
                                 BigDecimal percentualGlosa, String justificativa, Usuario autor) {
        medicoes.stream()
                .filter(m -> m.getIndicador().getId().equals(indicador.getId()))
                .findFirst()
                .ifPresentOrElse(
                        m -> m.atualizar(realizado, previsto, resultado, percentualGlosa, justificativa, autor),
                        () -> medicoes.add(new MedicaoIndicador(this, indicador, realizado, previsto, resultado,
                                percentualGlosa, justificativa, autor)));
    }

    public void aplicarGlosa(BigDecimal percentual, ValoresGlosa valores) {
        this.percentualGlosa = percentual;
        this.valorGlosa = valores.valorGlosa();
        this.valorFinal = valores.valorFinal();
    }

    /** Muda de fase e registra o evento no diário. */
    public void mudarFase(FaseOrdemServico nova, String acao, String observacao, Usuario autor, Instant quando) {
        FaseOrdemServico anterior = this.fase;
        this.fase = nova;
        registrarEvento(anterior, nova, acao, observacao, autor, quando);
    }

    public void registrarCriacao(Usuario autor, Instant quando) {
        registrarEvento(null, fase, "criacao", null, autor, quando);
    }

    private void registrarEvento(FaseOrdemServico de, FaseOrdemServico para, String acao, String observacao, Usuario autor, Instant quando) {
        eventos.add(new EventoOrdemServico(this, de, para, acao, observacao, autor, quando));
    }

    public void vincularEmpenho(Empenho empenho) {
        this.empenho = empenho;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public Empenho getEmpenho() {
        return empenho;
    }

    public String getNumero() {
        return numero;
    }

    public TipoOrdemServico getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public BigDecimal getPercentualGlosa() {
        return percentualGlosa;
    }

    public BigDecimal getValorGlosa() {
        return valorGlosa;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public FaseOrdemServico getFase() {
        return fase;
    }

    public LocalDate getPrazoEntrega() {
        return prazoEntrega;
    }

    public List<MedicaoIndicador> getMedicoes() {
        return medicoes;
    }

    public List<EventoOrdemServico> getEventos() {
        return eventos;
    }
}
