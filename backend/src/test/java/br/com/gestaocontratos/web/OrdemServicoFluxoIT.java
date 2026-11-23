package br.com.gestaocontratos.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaocontratos.dominio.Faixa;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Empenho;
import br.com.gestaocontratos.dominio.modelo.Indicador;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.EmpenhoRepositorio;
import br.com.gestaocontratos.repositorio.IndicadorRepositorio;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class OrdemServicoFluxoIT extends TesteDeIntegracao {

    @Autowired
    private EmpenhoRepositorio empenhos;
    @Autowired
    private IndicadorRepositorio indicadores;

    private Usuario gestor;
    private Usuario fiscalTecnico;
    private Usuario fiscalAdm;
    private Contrato contrato;
    private Empenho empenho;
    private Indicador indicador;

    @BeforeEach
    void preparar() {
        gestor = usuario(Perfil.GESTOR);
        fiscalTecnico = usuario(Perfil.GESTOR);
        fiscalAdm = usuario(Perfil.GESTOR);
        contrato = contrato(gestor, 300);
        designar(contrato, fiscalTecnico, TipoFiscal.TECNICO, null);
        designar(contrato, fiscalAdm, TipoFiscal.ADMINISTRATIVO, null);
        empenho = empenhos.save(new Empenho(contrato, "2026NE000001", 2026, new BigDecimal("150000.00"), null));
        indicador = indicadores.save(new Indicador(contrato, "IAP", "Atendimento de prazo", null, new BigDecimal("95"), List.of(
                faixa(95, 0), faixa(90, 2), faixa(80, 5), faixa(0, 10))));
    }

    private static Faixa faixa(int minimo, int glosa) {
        return new Faixa(BigDecimal.valueOf(minimo), BigDecimal.valueOf(glosa));
    }

    private Map<String, Object> dadosOs(double valor, Long empenhoId) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("contrato_id", contrato.getId());
        dados.put("tipo", "projeto");
        dados.put("descricao", "Sprint 12");
        dados.put("valor_estimado", valor);
        dados.put("prazo_entrega", "2026-06-01");
        dados.put("empenho_id", empenhoId);
        return dados;
    }

    private long emitirOs(double valor) throws Exception {
        Number id = ler(post("/api/ordens-servico", gestor, dadosOs(valor, empenho.getId())).andExpect(status().isCreated()),
                "$.data.id");
        return id.longValue();
    }

    private ResultActions avancar(long id, Usuario quem) throws Exception {
        return post("/api/ordens-servico/" + id + "/avancar", quem, Map.of());
    }

    private void levarAteFiscalizacaoTecnica(long id) throws Exception {
        avancar(id, gestor).andExpect(status().isOk()).andExpect(jsonPath("$.data.fase").value("em_execucao"));
        avancar(id, gestor).andExpect(status().isOk()).andExpect(jsonPath("$.data.fase").value("fiscalizacao_tecnica"));
    }

    private ResultActions medir(long id, double realizado) throws Exception {
        return put("/api/ordens-servico/" + id + "/medicoes", fiscalTecnico, Map.of("medicoes", List.of(
                Map.of("indicador_id", indicador.getId(), "realizado", realizado, "previsto", 100))));
    }

    @Test
    void emiteOsComNumeroSequencialPorAno() throws Exception {
        long primeira = emitirOs(100000);
        long segunda = emitirOs(100000);

        get("/api/ordens-servico/" + primeira, gestor).andExpect(jsonPath("$.data.numero").value("0001/2026"));
        get("/api/ordens-servico/" + segunda, gestor)
                .andExpect(jsonPath("$.data.numero").value("0002/2026"))
                .andExpect(jsonPath("$.data.fase").value("em_preparacao"))
                .andExpect(jsonPath("$.data.eventos[0].acao").value("criacao"))
                .andExpect(jsonPath("$.data.permissoes.avancar").value(true));
    }

    @Test
    void fluxoCompletoAteOEncerramentoComGlosa() throws Exception {
        long id = emitirOs(100000);
        levarAteFiscalizacaoTecnica(id);

        // Resultado de 92% cai na faixa de 2% de glosa.
        medir(id, 92)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medicoes[0].resultado").value(92.0))
                .andExpect(jsonPath("$.data.percentual_glosa").value(2.0))
                .andExpect(jsonPath("$.data.valor_glosa").value(2000.0))
                .andExpect(jsonPath("$.data.valor_final").value(98000.0));

        avancar(id, fiscalTecnico).andExpect(jsonPath("$.data.fase").value("fiscalizacao_administrativa"));
        avancar(id, fiscalAdm).andExpect(jsonPath("$.data.fase").value("recebimento_definitivo"));
        avancar(id, gestor).andExpect(jsonPath("$.data.fase").value("nota_fiscal"));
        avancar(id, gestor).andExpect(jsonPath("$.data.fase").value("analise_pagamento"));
        avancar(id, gestor).andExpect(jsonPath("$.data.fase").value("pagamento_autorizado"));
        avancar(id, gestor)
                .andExpect(jsonPath("$.data.fase").value("encerrada"))
                .andExpect(jsonPath("$.data.permissoes.avancar").value(false))
                .andExpect(jsonPath("$.data.eventos", hasSize(9))); // criação + 8 avanços

        get("/api/contratos/" + contrato.getId(), gestor).andExpect(jsonPath("$.data.empenhos[0].saldo").value(52000.0));
    }

    @Test
    void cadaFaseSoAvancaComOResponsavelCerto() throws Exception {
        long id = emitirOs(100000);

        avancar(id, fiscalTecnico)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Esta etapa cabe a: Gestor do contrato."));

        levarAteFiscalizacaoTecnica(id);
        medir(id, 100).andExpect(status().isOk());

        avancar(id, gestor)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Esta etapa cabe a: Fiscal técnico."));
    }

    @Test
    void fiscalComDesignacaoEncerradaNaoAtua() throws Exception {
        jdbc.update("update designacoes_fiscais set data_fim = ? where user_id = ?",
                relogioDaAplicacao.hoje().minusDays(1), fiscalTecnico.getId());
        long id = emitirOs(100000);
        levarAteFiscalizacaoTecnica(id);

        medir(id, 100).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exigeMedicaoDeTodosOsIndicadoresAntesDeAvancar() throws Exception {
        long id = emitirOs(100000);
        levarAteFiscalizacaoTecnica(id);

        avancar(id, fiscalTecnico)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Registre as medições dos indicadores antes de avançar: IAP."));
    }

    @Test
    void fiscalTecnicoDevolveParaCorrecao() throws Exception {
        long id = emitirOs(100000);
        levarAteFiscalizacaoTecnica(id);

        post("/api/ordens-servico/" + id + "/devolver", fiscalTecnico, Map.of("motivo", "Faltam testes automatizados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fase").value("em_execucao"))
                .andExpect(jsonPath("$.data.eventos[3].acao").value("devolucao"))
                .andExpect(jsonPath("$.data.eventos[3].observacao").value("Faltam testes automatizados"));
    }

    @Test
    void devolucaoExigeMotivo() throws Exception {
        long id = emitirOs(100000);
        levarAteFiscalizacaoTecnica(id);

        post("/api/ordens-servico/" + id + "/devolver", fiscalTecnico, Map.of())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.motivo").exists());
    }

    @Test
    void bloqueiaPagamentoSemSaldoNoEmpenho() throws Exception {
        long outra = emitirOs(100000);
        long id = emitirOs(100000);

        for (long os : new long[] {outra, id}) {
            levarAteFiscalizacaoTecnica(os);
            medir(os, 100);
            avancar(os, fiscalTecnico);
            avancar(os, fiscalAdm);
            avancar(os, gestor);
            avancar(os, gestor);
        }

        avancar(outra, gestor).andExpect(status().isOk()).andExpect(jsonPath("$.data.fase").value("pagamento_autorizado"));
        avancar(id, gestor)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Saldo insuficiente")))
                .andExpect(jsonPath("$.message", containsString("R$ 50.000,00")));
    }

    @Test
    void cancelamentoEncerraOFluxo() throws Exception {
        long id = emitirOs(100000);

        post("/api/ordens-servico/" + id + "/cancelar", gestor, Map.of("motivo", "Demanda suspensa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fase").value("cancelada"));

        avancar(id, gestor)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Esta OS já está finalizada."));
    }

    @Test
    void contratoSemIndicadoresUsaOValorEstimado() throws Exception {
        indicadores.delete(indicador);
        long id = emitirOs(80000);
        levarAteFiscalizacaoTecnica(id);

        avancar(id, fiscalTecnico)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valor_final").value(80000.0))
                .andExpect(jsonPath("$.data.percentual_glosa").value(0.0));
    }

    @Test
    void empenhoPrecisaSerDoMesmoContrato() throws Exception {
        Contrato outroContrato = contrato(gestor, 200);
        Empenho deOutroContrato = empenhos.save(new Empenho(outroContrato, "2026NE000999", 2026, new BigDecimal("10.00"), null));

        post("/api/ordens-servico", gestor, dadosOs(1000, deOutroContrato.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.empenho_id[0]").value("O empenho precisa ser do mesmo contrato da OS."));
    }

    @Test
    void listaFiltraPorFase() throws Exception {
        long id = emitirOs(1000);
        emitirOs(2000);
        avancar(id, gestor);

        get("/api/ordens-servico?fase=em_execucao", gestor)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].contrato.numero").value(contrato.getNumero()))
                .andExpect(jsonPath("$.data[0].eventos").doesNotExist());
    }
}
