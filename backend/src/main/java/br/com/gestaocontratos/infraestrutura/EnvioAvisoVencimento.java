package br.com.gestaocontratos.infraestrutura;

import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido;
import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido.Destinatario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia os e-mails depois que o registro do aviso foi gravado (após o commit) e fora da
 * thread da rotina diária. Só existe quando há servidor SMTP configurado (spring.mail.host).
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EnvioAvisoVencimento {

    private static final Logger LOG = LoggerFactory.getLogger(EnvioAvisoVencimento.class);

    private final JavaMailSender correio;
    private final String remetente;
    private final String urlFrontend;

    public EnvioAvisoVencimento(JavaMailSender correio,
                                @Value("${gestao-contratos.avisos.remetente}") String remetente,
                                @Value("${gestao-contratos.frontend-url}") String urlFrontend) {
        this.correio = correio;
        this.remetente = remetente;
        this.urlFrontend = urlFrontend;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void enviar(AvisoVencimentoEmitido aviso) {
        for (Destinatario destinatario : aviso.destinatarios()) {
            MensagemAvisoVencimento mensagem = MensagemAvisoVencimento.de(aviso, destinatario, urlFrontend);
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(remetente);
            email.setTo(mensagem.para());
            email.setSubject(mensagem.assunto());
            email.setText(mensagem.corpo());
            try {
                correio.send(email);
            } catch (MailException e) {
                LOG.error("Falha ao enviar aviso do contrato {} para {}", aviso.numero(), destinatario.email(), e);
            }
        }
    }
}
