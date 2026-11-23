package br.com.gestaocontratos.infraestrutura;

import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido;
import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido.Destinatario;
import br.com.gestaocontratos.dominio.Formatos;

/** Texto do e-mail de aviso de vencimento. */
public record MensagemAvisoVencimento(String para, String assunto, String corpo) {

    public static MensagemAvisoVencimento de(AvisoVencimentoEmitido aviso, Destinatario destinatario, String urlFrontend) {
        String vencimento = aviso.dataFim().format(Formatos.DATA);
        String assunto = "[Aviso de %d dias] Contrato %s vence em %s".formatted(aviso.marco(), aviso.numero(), vencimento);
        String link = urlFrontend.replaceAll("/+$", "") + "/contratos/" + aviso.contratoId();
        String corpo = """
                Olá, %s.

                O contrato %s (%s) vence em %d dia(s), no dia %s.

                Objeto: %s

                Avalie a necessidade de prorrogação ou de nova contratação a tempo de cumprir os prazos do processo.

                Abrir o contrato: %s
                """.formatted(destinatario.nome(), aviso.numero(), aviso.contratada(), aviso.diasRestantes(), vencimento,
                aviso.objeto(), link);
        return new MensagemAvisoVencimento(destinatario.email(), assunto, corpo);
    }
}
