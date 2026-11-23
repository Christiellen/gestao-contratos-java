package br.com.gestaocontratos.aplicacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Corpos das requisições. Os campos chegam em snake_case (valor_global) e são validados
 * nos serviços, que também conferem as regras que dependem do banco (número único, empenho do contrato...).
 */
public final class Formularios {

    private Formularios() {
    }

    public record LoginForm(String email, String password) {
    }

    public record ContratoForm(String numero, String objeto, String contratada, String cnpj, BigDecimal valorGlobal,
                               LocalDate dataInicio, LocalDate dataFim, Long gestorId, Long fiscalId) {
    }

    public record AditivoForm(String tipo, LocalDate dataAssinatura, LocalDate novaDataFim, BigDecimal valorAcrescimo,
                              String descricao) {
    }

    public record EmpenhoForm(String numero, Integer ano, BigDecimal valor, String descricao) {
    }

    public record FaixaForm(BigDecimal minimo, BigDecimal glosa) {
    }

    public record IndicadorForm(String sigla, String nome, String descricao, BigDecimal meta, List<FaixaForm> faixas) {
    }

    public record FiscalForm(Long userId, String portaria, String tipo, String papel, LocalDate dataInicio, LocalDate dataFim) {
    }

    public record PrepostoForm(String nome, String email, String telefone, String papel, LocalDate dataInicio, LocalDate dataFim) {
    }

    public record OrdemServicoForm(Long contratoId, String tipo, String descricao, BigDecimal valorEstimado,
                                   LocalDate prazoEntrega, Long empenhoId) {
    }

    public record MedicaoForm(Long indicadorId, BigDecimal realizado, BigDecimal previsto, String justificativa) {
    }

    public record MedicoesForm(List<MedicaoForm> medicoes) {
    }

    public record ObservacaoForm(String observacao) {
    }

    public record MotivoForm(String motivo) {
    }

    public record VinculoEmpenhoForm(Long empenhoId) {
    }
}
