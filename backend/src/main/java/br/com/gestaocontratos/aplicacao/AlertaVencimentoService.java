package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido.Destinatario;
import br.com.gestaocontratos.dominio.RegrasVencimento;
import br.com.gestaocontratos.dominio.modelo.AlertaVencimento;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.AlertaVencimentoRepositorio;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import br.com.gestaocontratos.repositorio.DesignacaoFiscalRepositorio;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envia os avisos de vencimento (ao gestor, ao fiscal principal e aos fiscais
 * designados em vigor) e registra cada envio.
 *
 * O registro em alertas_vencimento torna a rotina idempotente: rodar duas vezes no mesmo
 * dia não duplica e-mails. Quando um aditivo prorroga o contrato, a data de fim muda e um
 * novo ciclo de avisos começa. Cada contrato é processado na sua própria transação, para
 * que um problema em um não impeça os demais.
 */
@Service
public class AlertaVencimentoService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertaVencimentoService.class);

    private final RegrasVencimento vencimento = new RegrasVencimento();
    private final ContratoRepositorio contratos;
    private final DesignacaoFiscalRepositorio designacoes;
    private final AlertaVencimentoRepositorio alertas;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate transacao;
    private final Relogio relogio;

    public AlertaVencimentoService(ContratoRepositorio contratos, DesignacaoFiscalRepositorio designacoes,
                                   AlertaVencimentoRepositorio alertas, ApplicationEventPublisher eventos,
                                   TransactionTemplate transacao, Relogio relogio) {
        this.contratos = contratos;
        this.designacoes = designacoes;
        this.alertas = alertas;
        this.eventos = eventos;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public record Resultado(int verificados, int enviados) {
    }

    public Resultado processar() {
        return processar(relogio.hoje());
    }

    public Resultado processar(LocalDate hoje) {
        int maiorMarco = RegrasVencimento.MARCOS.getFirst();
        List<Contrato> naJanela = contratos.vencendoEntre(hoje, hoje.plusDays(maiorMarco));

        int enviados = 0;
        for (Contrato contrato : naJanela) {
            try {
                if (Boolean.TRUE.equals(transacao.execute(status -> processarContrato(contrato, hoje)))) {
                    enviados++;
                }
            } catch (DataIntegrityViolationException e) {
                // Outra execução concorrente já registrou este aviso.
                LOG.info("Aviso do contrato {} já registrado por outra execução.", contrato.getNumero());
            }
        }
        LOG.info("Contratos verificados: {}. Avisos enviados: {}.", naJanela.size(), enviados);
        return new Resultado(naJanela.size(), enviados);
    }

    private boolean processarContrato(Contrato contrato, LocalDate hoje) {
        OptionalInt marco = vencimento.marcoAplicavel(contrato.getDataFim(), hoje);
        if (marco.isEmpty()
                || alertas.existsByContratoIdAndMarcoAndDataFimReferencia(contrato.getId(), marco.getAsInt(), contrato.getDataFim())) {
            return false;
        }

        List<Destinatario> destinatarios = destinatarios(contrato, hoje);
        if (destinatarios.isEmpty()) {
            return false;
        }

        alertas.saveAndFlush(new AlertaVencimento(contrato.getId(), marco.getAsInt(), contrato.getDataFim(),
                destinatarios.size(), relogio.agora()));
        eventos.publishEvent(new AvisoVencimentoEmitido(contrato.getId(), contrato.getNumero(), contrato.getContratada(),
                contrato.getObjeto(), contrato.getDataFim(), marco.getAsInt(),
                vencimento.diasParaVencer(contrato.getDataFim(), hoje), destinatarios));
        return true;
    }

    /** Gestor, fiscal principal e fiscais designados em vigor, sem repetir a mesma pessoa. */
    private List<Destinatario> destinatarios(Contrato contrato, LocalDate hoje) {
        Map<Long, Destinatario> unicos = new LinkedHashMap<>();
        Stream.concat(
                        Stream.of(contrato.getGestor(), contrato.getFiscal()),
                        designacoes.emVigor(contrato.getId(), hoje).stream().map(DesignacaoFiscal::getUsuario))
                .filter(Objects::nonNull)
                .forEach((Usuario u) -> unicos.putIfAbsent(u.getId(), new Destinatario(u.getId(), u.getNome(), u.getEmail())));
        return List.copyOf(unicos.values());
    }
}
