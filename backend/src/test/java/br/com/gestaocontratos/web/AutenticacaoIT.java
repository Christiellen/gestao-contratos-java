package br.com.gestaocontratos.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AutenticacaoIT extends TesteDeIntegracao {

    @Test
    void loginComCredenciaisValidasRetornaToken() throws Exception {
        Usuario ana = usuario(Perfil.GESTOR, "ana@exemplo.gov.br");

        login("ana@exemplo.gov.br", "password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.usuario.id").value(ana.getId().intValue()))
                .andExpect(jsonPath("$.usuario.nome").exists())
                .andExpect(jsonPath("$.usuario.perfil").value("gestor"))
                .andExpect(jsonPath("$.usuario.perfil_rotulo").value("Gestor de contratos"));
    }

    @Test
    void loginComSenhaErradaFalhaSemDizerQualCampoErrou() throws Exception {
        usuario(Perfil.CONSULTA, "ana@exemplo.gov.br");

        login("ana@exemplo.gov.br", "errada")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.email[0]").value("E-mail ou senha inválidos."));
        login("ninguem@exemplo.gov.br", "password")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.email[0]").value("E-mail ou senha inválidos."));
    }

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        getComToken("/api/contratos", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado."));
        getComToken("/api/me", null).andExpect(status().isUnauthorized());
        getComToken("/api/me", "token-inventado").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenPermiteAcessarELogoutORevoga() throws Exception {
        usuario(Perfil.CONSULTA, "ana@exemplo.gov.br");
        String token = ler(login("ana@exemplo.gov.br", "password"), "$.token");

        getComToken("/api/me", token).andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("ana@exemplo.gov.br"));
        postComToken("/api/logout", token).andExpect(status().isNoContent());
        getComToken("/api/me", token).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiraEmOitoHoras() throws Exception {
        usuario(Perfil.CONSULTA, "ana@exemplo.gov.br");
        String token = ler(login("ana@exemplo.gov.br", "password"), "$.token");

        relogio.avancar(Duration.ofHours(8));
        getComToken("/api/me", token).andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueiaExcessoDeTentativasDeLogin() throws Exception {
        usuario(Perfil.CONSULTA, "ana@exemplo.gov.br");
        for (int i = 0; i < 5; i++) {
            login("ana@exemplo.gov.br", "errada");
        }

        login("ana@exemplo.gov.br", "password").andExpect(status().isTooManyRequests());

        relogio.avancar(Duration.ofMinutes(1).plusSeconds(1));
        login("ana@exemplo.gov.br", "password").andExpect(status().isOk());
    }
}
