package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.FaseOrdemServico;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Linha do diário da OS: quem mudou a fase, de qual para qual, quando e por quê. */
@Entity
@Table(name = "eventos_ordem_servico")
public class EventoOrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @Convert(converter = ConversorComValor.DeFase.class)
    @Column(name = "fase_anterior")
    private FaseOrdemServico faseAnterior;

    @Convert(converter = ConversorComValor.DeFase.class)
    @Column(name = "fase_nova", nullable = false)
    private FaseOrdemServico faseNova;

    @Column(nullable = false)
    private String acao;

    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    protected EventoOrdemServico() {
    }

    public EventoOrdemServico(OrdemServico ordemServico, FaseOrdemServico faseAnterior, FaseOrdemServico faseNova,
                              String acao, String observacao, Usuario usuario, Instant ocorridoEm) {
        this.ordemServico = ordemServico;
        this.faseAnterior = faseAnterior;
        this.faseNova = faseNova;
        this.acao = acao;
        this.observacao = observacao;
        this.usuario = usuario;
        this.ocorridoEm = ocorridoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getOrdemServicoId() {
        return ordemServico.getId();
    }

    public FaseOrdemServico getFaseAnterior() {
        return faseAnterior;
    }

    public FaseOrdemServico getFaseNova() {
        return faseNova;
    }

    public String getAcao() {
        return acao;
    }

    public String getObservacao() {
        return observacao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }
}
