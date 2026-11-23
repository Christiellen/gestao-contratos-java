package br.com.gestaocontratos.aplicacao.dto;

import br.com.gestaocontratos.dominio.Faixa;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respostas da API. Os nomes viram snake_case no JSON (valorGlobal -> valor_global),
 * reproduzindo os campos da versão em PHP para o front-end Angular continuar igual.
 */
public final class Visoes {

    private Visoes() {
    }

    public record UsuarioVisao(Long id, String nome, String email, String perfil, String perfilRotulo) {
    }

    public record AditivoVisao(Long id, int numero, String tipo, LocalDate dataAssinatura, LocalDate dataFimAnterior,
                               LocalDate novaDataFim, BigDecimal valorAcrescimo, String descricao, OffsetDateTime registradoEm) {
    }

    public record EmpenhoVisao(Long id, String numero, int ano, BigDecimal valor, BigDecimal utilizado, BigDecimal saldo,
                               String descricao) {
    }

    public record IndicadorVisao(Long id, String sigla, String nome, BigDecimal meta, List<Faixa> faixas) {
    }

    public record FiscalVisao(Long id, String nome, String portaria, String tipo, String tipoRotulo, String papel,
                              LocalDate dataInicio, LocalDate dataFim) {
    }

    public record PrepostoVisao(Long id, String nome, String email, String telefone, String papel, boolean ativo,
                                LocalDate dataInicio, LocalDate dataFim) {
    }

    /** As listas só aparecem no detalhe do contrato (ficam fora do JSON na listagem). */
    public record ContratoVisao(
            Long id, String numero, String objeto, String contratada, String cnpj, BigDecimal valorGlobal,
            LocalDate dataInicio, LocalDate dataFim, int diasParaVencer, String situacao, String situacaoRotulo,
            UsuarioVisao gestor, UsuarioVisao fiscal,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<AditivoVisao> aditivos,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<EmpenhoVisao> empenhos,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<IndicadorVisao> indicadores,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<FiscalVisao> fiscais,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<PrepostoVisao> prepostos,
            boolean podeEditar, boolean podeExcluir) {
    }

    public record PainelVisao(Map<String, Long> totais, BigDecimal valorTotalVigente, List<ContratoVisao> proximosVencimentos) {
    }

    public record ContratoResumo(Long id, String numero, String contratada, List<IndicadorVisao> indicadores) {
    }

    public record EmpenhoResumo(Long id, String numero, BigDecimal saldo) {
    }

    public record MedicaoVisao(Long indicadorId, String sigla, BigDecimal realizado, BigDecimal previsto, BigDecimal resultado,
                               BigDecimal percentualGlosa, String justificativa) {
    }

    public record EventoVisao(String acao, String de, String para, String observacao, String usuario, OffsetDateTime ocorridoEm) {
    }

    public record Permissoes(boolean avancar, boolean medir, boolean devolver, boolean cancelar) {
    }

    public record OrdemServicoVisao(
            Long id, String numero, String tipo, String tipoRotulo, String descricao, BigDecimal valorEstimado,
            BigDecimal percentualGlosa, BigDecimal valorGlosa, BigDecimal valorFinal, LocalDate prazoEntrega,
            String fase, String faseRotulo, String proximaFase, String proximaFaseRotulo, String responsavel,
            ContratoResumo contrato, EmpenhoResumo empenho,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<MedicaoVisao> medicoes,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<EventoVisao> eventos,
            Permissoes permissoes) {
    }

    public record TransicaoVisao(String de, String para, String rotulo, int quantidade, long mediaSegundos, String mediaTexto) {
    }

    public record TempoMedioVisao(LocalDate inicio, LocalDate fim, List<TransicaoVisao> transicoes) {
    }

    public record PendenciaFaseVisao(String fase, String rotulo, int quantidade, BigDecimal valor) {
    }

    public record ValoresPendentesVisao(List<PendenciaFaseVisao> fases, BigDecimal total) {
    }

    public record LoginVisao(String token, UsuarioVisao usuario) {
    }
}
