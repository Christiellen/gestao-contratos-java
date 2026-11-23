package br.com.gestaocontratos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.AditivoRepositorio;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AditivoIT extends TesteDeIntegracao {

    @Autowired
    private AditivoRepositorio aditivos;

    private Usuario gestor;
    private Contrato contrato;
    private String url;

    @BeforeEach
    void preparar() {
        relogio.irPara("2026-03-10T09:00:00");
        gestor = usuario(Perfil.GESTOR);
        contrato = contratos.save(new Contrato("50/2025", "Sustentação", "Empresa Exemplo Ltda", "11222333000181",
                new BigDecimal("1000000.00"), LocalDate.parse("2025-04-01"), LocalDate.parse("2026-03-31"), gestor, null));
        url = "/api/contratos/" + contrato.getId() + "/aditivos";
    }

    private Contrato recarregado() {
        return contratos.findById(contrato.getId()).orElseThrow();
    }

    @Test
    void aditivoDePrazoProrrogaAVigencia() throws Exception {
        post(url, gestor, Map.of("tipo", "prazo", "data_assinatura", "2026-03-05", "nova_data_fim", "2027-03-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value(1))
                .andExpect(jsonPath("$.data.data_fim_anterior").value("2026-03-31"))
                .andExpect(jsonPath("$.data.nova_data_fim").value("2027-03-31"));

        assertThat(recarregado().getDataFim()).isEqualTo("2027-03-31");
    }

    @Test
    void aditivoDeValorSomaAoValorGlobalENumeraEmSequencia() throws Exception {
        post(url, gestor, Map.of("tipo", "valor", "data_assinatura", "2026-01-10", "valor_acrescimo", 250000.10))
                .andExpect(status().isCreated());
        post(url, gestor, Map.of("tipo", "valor", "data_assinatura", "2026-02-10", "valor_acrescimo", -50000.05))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value(2));

        Contrato c = recarregado();
        assertThat(c.getValorGlobal()).isEqualByComparingTo("1200000.05");
        assertThat(c.getDataFim()).isEqualTo("2026-03-31");
    }

    @Test
    void rejeitaProrrogacaoParaDataAnteriorAVigencia() throws Exception {
        post(url, gestor, Map.of("tipo", "prazo", "data_assinatura", "2026-03-05", "nova_data_fim", "2026-03-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.nova_data_fim[0]")
                        .value("A nova data de fim deve ser posterior à vigência atual (31/03/2026)."));

        assertThat(aditivos.countByContratoId(contrato.getId())).isZero();
    }

    @Test
    void exigeCamposConformeOTipo() throws Exception {
        post(url, gestor, Map.of("tipo", "prazo_valor", "data_assinatura", "2026-03-05"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.nova_data_fim").exists())
                .andExpect(jsonPath("$.errors.valor_acrescimo").exists());
    }

    @Test
    void outroGestorNaoRegistraAditivo() throws Exception {
        post(url, usuario(Perfil.GESTOR), Map.of("tipo", "prazo", "data_assinatura", "2026-03-05", "nova_data_fim", "2027-03-31"))
                .andExpect(status().isForbidden());
    }
}
