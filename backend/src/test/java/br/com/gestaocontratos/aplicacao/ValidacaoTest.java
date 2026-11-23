package br.com.gestaocontratos.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gestaocontratos.dominio.TipoAditivo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValidacaoTest {

    @Test
    void acumulaOsErrosEResumeAMensagem() {
        Validacao v = new Validacao();
        v.obrigatorio("numero", "número", " ");
        v.entre("valor", "valor", new BigDecimal("0"), "0.01", "100");
        assertThat(v.opcao("tipo", "tipo", "inexistente", TipoAditivo.class)).isNull();

        assertThatThrownBy(v::lancarSeHouverErros)
                .isInstanceOfSatisfying(ValidacaoException.class, e -> {
                    assertThat(e.getErros()).containsOnlyKeys("numero", "valor", "tipo");
                    assertThat(e.getMessage()).isEqualTo("O campo número é obrigatório. (e mais 2 erros)");
                });
    }

    @Test
    void registraSoOPrimeiroErroDeCadaCampo() {
        Validacao v = new Validacao();
        v.obrigatorio("email", "e-mail", null);
        v.email("email", null);
        v.se(true, "email", "outro erro");
        assertThatThrownBy(v::lancarSeHouverErros)
                .isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getErros().get("email")).hasSize(1));
    }

    @Test
    void semErrosNaoLanca() {
        Validacao v = new Validacao();
        v.obrigatorio("numero", "número", "12/2026");
        v.lancarSeHouverErros();
        assertThat(v.semErro("numero")).isTrue();
    }
}
