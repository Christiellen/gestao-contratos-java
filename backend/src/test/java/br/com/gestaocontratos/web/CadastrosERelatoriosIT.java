package br.com.gestaocontratos.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaocontratos.dominio.CalculadoraGlosa.ValoresGlosa;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoOrdemServico;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.OrdemServicoRepositorio;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class CadastrosERelatoriosIT extends TesteDeIntegracao {

    @Autowired
    private OrdemServicoRepositorio ordens;

    @Autowired
    private TransactionTemplate transacao;

    private final AtomicInteger numero = new AtomicInteger();

    @Test
    void gestorCadastraEmpenhoIndicadorFiscalEPreposto() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Usuario fiscal = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);
        String base = "/api/contratos/" + contrato.getId();

        post(base + "/empenhos", gestor, Map.of("numero", "2026NE000123", "ano", 2026, "valor", 500000))
                .andExpect(status().isCreated());
        post(base + "/indicadores", gestor, Map.of("sigla", "IAP", "nome", "Atendimento de prazo", "meta", 95,
                "faixas", List.of(Map.of("minimo", 0, "glosa", 5), Map.of("minimo", 95, "glosa", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.faixas[0].minimo").value(95));
        post(base + "/fiscais", gestor, Map.of("user_id", fiscal.getId(), "portaria", "Portaria nº 10/2026", "tipo", "tecnico",
                "papel", "titular", "data_inicio", "2026-01-01"))
                .andExpect(status().isCreated());
        post(base + "/prepostos", gestor, Map.of("nome", "Maria Preposta", "email", "maria@empresa.com.br", "papel", "titular",
                "data_inicio", "2026-01-01"))
                .andExpect(status().isCreated());

        get(base, gestor)
                .andExpect(jsonPath("$.data.empenhos[0].saldo").value(500000.0))
                .andExpect(jsonPath("$.data.indicadores[0].sigla").value("IAP"))
                .andExpect(jsonPath("$.data.fiscais[0].tipo_rotulo").value("Fiscal técnico"))
                .andExpect(jsonPath("$.data.prepostos[0].ativo").value(true));
    }

    @Test
    void validaFaixasDoIndicador() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);

        post("/api/contratos/" + contrato.getId() + "/indicadores", gestor, Map.of("sigla", "IAP", "nome", "Prazo", "meta", 95,
                "faixas", List.of(Map.of("minimo", 90, "glosa", 2), Map.of("minimo", 90, "glosa", 120))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors['faixas.1.minimo']").exists())
                .andExpect(jsonPath("$.errors['faixas.1.glosa']").exists());
    }

    @Test
    void empenhoComNumeroRepetidoNoContratoERejeitado() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);
        Map<String, Object> dados = Map.of("numero", "2026NE000123", "ano", 2026, "valor", 1000);
        String url = "/api/contratos/" + contrato.getId() + "/empenhos";

        post(url, gestor, dados).andExpect(status().isCreated());
        post(url, gestor, dados).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors.numero").exists());
    }

    @Test
    void perfilConsultaNaoCadastra() throws Exception {
        Contrato contrato = contrato(usuario(Perfil.GESTOR), 300);
        post("/api/contratos/" + contrato.getId() + "/empenhos", usuario(Perfil.CONSULTA),
                Map.of("numero", "X", "ano", 2026, "valor", 1))
                .andExpect(status().isForbidden());
    }

    private OrdemServico ordem(Contrato contrato, Usuario autor, FaseOrdemServico fase, String estimado, String valorFinal) {
        return transacao.execute(status -> {
            OrdemServico os = new OrdemServico(contrato, null, "%04d/2026".formatted(numero.incrementAndGet()), TipoOrdemServico.PROJETO,
                    "Demanda", new BigDecimal(estimado), LocalDate.now(relogio), autor);
            if (valorFinal != null) {
                os.aplicarGlosa(BigDecimal.ZERO, new ValoresGlosa(
                        BigDecimal.ZERO, new BigDecimal(valorFinal)));
            }
            if (fase != FaseOrdemServico.EM_PREPARACAO) {
                os.mudarFase(fase, "avanco", null, autor, relogio.instant());
            }
            return ordens.save(os);
        });
    }

    @Test
    void tempoMedioPorTransicao() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);
        Instant inicio = LocalDate.now(relogio).minusDays(10).atStartOfDay(relogio.getZone()).toInstant();

        // Duas OS: a primeira leva 2 dias na execução, a segunda 4 dias. Média: 3 dias.
        for (int dias : new int[] {2, 4}) {
            OrdemServico os = ordem(contrato, gestor, FaseOrdemServico.EM_PREPARACAO, "1000", null);
            transacao.executeWithoutResult(status -> {
                OrdemServico gerenciada = ordens.findById(os.getId()).orElseThrow();
                gerenciada.mudarFase(FaseOrdemServico.EM_EXECUCAO, "avanco", null, gestor, inicio);
                gerenciada.mudarFase(FaseOrdemServico.FISCALIZACAO_TECNICA, "avanco", null, gestor,
                        inicio.plus(Duration.ofDays(dias)));
            });
        }

        get("/api/relatorios/tempo-medio", gestor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transicoes", hasSize(1)))
                .andExpect(jsonPath("$.transicoes[0].rotulo").value("Em execução → Fiscalização técnica"))
                .andExpect(jsonPath("$.transicoes[0].quantidade").value(2))
                .andExpect(jsonPath("$.transicoes[0].media_texto").value("3d 0h 0min"));
    }

    @Test
    void valoresPendentesPorFase() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);
        ordem(contrato, gestor, FaseOrdemServico.NOTA_FISCAL, "1000", "900");
        ordem(contrato, gestor, FaseOrdemServico.NOTA_FISCAL, "500", null);
        ordem(contrato, gestor, FaseOrdemServico.ENCERRADA, "9999", "9999");

        get("/api/relatorios/valores-pendentes", gestor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fases[?(@.fase == 'nota_fiscal')].quantidade").value(2))
                .andExpect(jsonPath("$.fases[?(@.fase == 'nota_fiscal')].valor").value(1400.0))
                .andExpect(jsonPath("$.total").value(1400.0));
    }
}
