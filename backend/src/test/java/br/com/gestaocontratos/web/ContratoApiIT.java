package br.com.gestaocontratos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContratoApiIT extends TesteDeIntegracao {

    private Map<String, Object> dadosValidos(Usuario gestor) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("numero", "12/2026");
        dados.put("objeto", "Sustentação de sistemas");
        dados.put("contratada", "Empresa Exemplo Ltda");
        dados.put("cnpj", "11.222.333/0001-81");
        dados.put("valor_global", 1250000.50);
        dados.put("data_inicio", "2026-01-01");
        dados.put("data_fim", "2027-01-01");
        dados.put("gestor_id", gestor.getId());
        return dados;
    }

    @Test
    void gestorCadastraContratoECnpjESalvoSemMascara() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);

        post("/api/contratos", gestor, dadosValidos(gestor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value("12/2026"))
                .andExpect(jsonPath("$.data.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.data.valor_global").value(1250000.50))
                .andExpect(jsonPath("$.data.gestor.id").value(gestor.getId().intValue()))
                .andExpect(jsonPath("$.data.pode_editar").value(true))
                .andExpect(jsonPath("$.data.pode_excluir").value(false));

        assertThat(jdbc.queryForObject("select cnpj from contratos where numero = '12/2026'", String.class))
                .isEqualTo("11222333000181");
    }

    @Test
    void perfilConsultaNaoCadastra() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        post("/api/contratos", usuario(Perfil.CONSULTA), dadosValidos(gestor)).andExpect(status().isForbidden());
    }

    @Test
    void validaCnpjDatasENumeroUnico() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        post("/api/contratos", gestor, dadosValidos(gestor)).andExpect(status().isCreated());

        Map<String, Object> dados = dadosValidos(gestor);
        dados.put("cnpj", "11.222.333/0001-82");
        dados.put("data_fim", "2025-12-31");

        post("/api/contratos", gestor, dados)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.numero").exists())
                .andExpect(jsonPath("$.errors.cnpj").exists())
                .andExpect(jsonPath("$.errors.data_fim").exists())
                .andExpect(jsonPath("$.message").value("Já existe um contrato com este número. (e mais 2 erros)"));
    }

    @Test
    void dataEmFormatoInvalidoApontaOCampo() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Map<String, Object> dados = dadosValidos(gestor);
        dados.put("data_fim", "31/12/2026");

        post("/api/contratos", gestor, dados)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.data_fim").exists());
    }

    @Test
    void gestorSoAlteraOsContratosQueGere() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Usuario outro = usuario(Perfil.GESTOR);
        Contrato meu = contrato(gestor, 300);
        Contrato alheio = contrato(outro, 300);

        Map<String, Object> dados = dadosValidos(gestor);
        dados.put("numero", meu.getNumero());
        dados.put("objeto", "Novo objeto");
        put("/api/contratos/" + meu.getId(), gestor, dados)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objeto").value("Novo objeto"));

        Map<String, Object> dadosAlheio = dadosValidos(outro);
        dadosAlheio.put("numero", alheio.getNumero());
        put("/api/contratos/" + alheio.getId(), gestor, dadosAlheio).andExpect(status().isForbidden());
    }

    @Test
    void somenteAdministradorExcluiEAExclusaoELogica() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        Contrato contrato = contrato(gestor, 300);

        delete("/api/contratos/" + contrato.getId(), gestor).andExpect(status().isForbidden());
        delete("/api/contratos/" + contrato.getId(), usuario(Perfil.ADMINISTRADOR)).andExpect(status().isNoContent());

        get("/api/contratos/" + contrato.getId(), gestor).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_at is not null from contratos where id = ?", Boolean.class, contrato.getId()))
                .isTrue();
    }

    @Test
    void listaFiltraPorSituacaoEBusca() throws Exception {
        Usuario consulta = usuario(Perfil.CONSULTA);
        Usuario gestor = usuario(Perfil.GESTOR);
        contrato(gestor, null, -5, "Alfa Sistemas", "Serviços diversos");
        contrato(gestor, null, 40, "Beta Nuvem", "Serviços diversos");
        contrato(gestor, null, 80, "Gama Dados", "Serviços diversos");
        contrato(gestor, null, 300, "Delta Redes", "Serviços diversos");

        get("/api/contratos?situacao=a_vencer", consulta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].contratada").value("Beta Nuvem"))
                .andExpect(jsonPath("$.data[0].dias_para_vencer").value(40))
                .andExpect(jsonPath("$.data[0].aditivos").doesNotExist())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.meta.current_page").value(1));

        get("/api/contratos?situacao=vencido", consulta)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].situacao").value("vencido"));
        get("/api/contratos?situacao=vigente", consulta).andExpect(jsonPath("$.data", hasSize(1)));
        get("/api/contratos?busca=NUVEM", consulta).andExpect(jsonPath("$.data", hasSize(1)));
        get("/api/contratos?situacao=qualquer", consulta).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listaEPaginada() throws Exception {
        Usuario gestor = usuario(Perfil.GESTOR);
        for (int i = 0; i < 3; i++) {
            contrato(gestor, 100 + i);
        }

        get("/api/contratos?por_pagina=2&page=2", gestor)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.meta.current_page").value(2))
                .andExpect(jsonPath("$.meta.last_page").value(2))
                .andExpect(jsonPath("$.meta.per_page").value(2))
                .andExpect(jsonPath("$.meta.total").value(3));
    }

    @Test
    void painelResumePorSituacao() throws Exception {
        Usuario consulta = usuario(Perfil.CONSULTA);
        Usuario gestor = usuario(Perfil.GESTOR);
        contrato(gestor, -1);
        contrato(gestor, 10);
        contrato(gestor, 20);
        contrato(gestor, 200);

        get("/api/painel", consulta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totais.vencido").value(1))
                .andExpect(jsonPath("$.totais.a_vencer").value(2))
                .andExpect(jsonPath("$.totais.vigente").value(1))
                .andExpect(jsonPath("$.valor_total_vigente").value(3000000.0))
                .andExpect(jsonPath("$.proximos_vencimentos", hasSize(2)))
                .andExpect(jsonPath("$.proximos_vencimentos[0].dias_para_vencer").value(10));
    }

    @Test
    void listaGestoresEFiscaisParaOsFormularios() throws Exception {
        usuario(Perfil.CONSULTA);
        Usuario gestor = usuario(Perfil.GESTOR);
        usuario(Perfil.ADMINISTRADOR);

        get("/api/usuarios", gestor).andExpect(jsonPath("$.data", hasSize(2)));
    }
}
