package br.com.gestaocontratos.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Registro de um aviso de vencimento enviado. A restrição única (contrato, marco, vigência)
 * torna a rotina idempotente: o mesmo aviso nunca sai duas vezes.
 */
@Entity
@Table(name = "alertas_vencimento")
public class AlertaVencimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrato_id", nullable = false)
    private Long contratoId;

    @Column(nullable = false)
    private int marco;

    @Column(name = "data_fim_referencia", nullable = false)
    private LocalDate dataFimReferencia;

    @Column(nullable = false)
    private int destinatarios;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    protected AlertaVencimento() {
    }

    public AlertaVencimento(Long contratoId, int marco, LocalDate dataFimReferencia, int destinatarios, Instant enviadoEm) {
        this.contratoId = contratoId;
        this.marco = marco;
        this.dataFimReferencia = dataFimReferencia;
        this.destinatarios = destinatarios;
        this.enviadoEm = enviadoEm;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public int getMarco() {
        return marco;
    }

    public LocalDate getDataFimReferencia() {
        return dataFimReferencia;
    }

    public int getDestinatarios() {
        return destinatarios;
    }
}
