package br.com.gestaocontratos.infraestrutura;

import br.com.gestaocontratos.aplicacao.AlertaVencimentoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Verificação diária dos vencimentos (avisos de 90, 60 e 30 dias), às 7h de Brasília.
 * Com mais de uma instância da API, cada uma roda a rotina, mas a restrição única em
 * alertas_vencimento garante um único aviso por marco.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "gestao-contratos.avisos.agendamento-habilitado", havingValue = "true", matchIfMissing = true)
public class AgendamentoAvisos {

    private final AlertaVencimentoService avisos;

    public AgendamentoAvisos(AlertaVencimentoService avisos) {
        this.avisos = avisos;
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    public void verificarVencimentos() {
        avisos.processar();
    }
}
