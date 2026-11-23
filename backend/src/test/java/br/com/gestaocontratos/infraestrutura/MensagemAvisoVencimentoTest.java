package br.com.gestaocontratos.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido;
import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido.Destinatario;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MensagemAvisoVencimentoTest {

    @Test
    void emailInformaContratoMarcoEData() {
        Destinatario ana = new Destinatario(1L, "Ana", "ana@exemplo.gov.br");
        AvisoVencimentoEmitido aviso = new AvisoVencimentoEmitido(7L, "12/2026", "Empresa Exemplo", "Sustentação",
                LocalDate.of(2026, 4, 9), 30, 30, List.of(ana));

        MensagemAvisoVencimento mensagem = MensagemAvisoVencimento.de(aviso, ana, "http://localhost:8080/");

        assertThat(mensagem.para()).isEqualTo("ana@exemplo.gov.br");
        assertThat(mensagem.assunto()).isEqualTo("[Aviso de 30 dias] Contrato 12/2026 vence em 09/04/2026");
        assertThat(mensagem.corpo()).contains("Olá, Ana.").contains("http://localhost:8080/contratos/7");
    }
}
