package br.com.gestaocontratos.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import br.com.gestaocontratos.aplicacao.AvisoVencimentoEmitido.Destinatario;
import br.com.gestaocontratos.aplicacao.dto.Formularios.AditivoForm;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.AlertaVencimento;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.AlertaVencimentoRepositorio;
import br.com.gestaocontratos.suporte.TesteDeIntegracao;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** A rotina diária de avisos (o equivalente ao comando contratos:alertar-vencimentos). */
@RecordApplicationEvents
class AlertaVencimentoIT extends TesteDeIntegracao {

    @Autowired
    private AlertaVencimentoService servico;
    @Autowired
    private AditivoService aditivos;
    @Autowired
    private AlertaVencimentoRepositorio alertas;
    @Autowired
    private ApplicationEvents eventos;

    private Usuario gestor;
    private Usuario fiscal;

    @BeforeEach
    void preparar() {
        relogio.irPara("2026-03-10T07:00:00");
        gestor = usuario(Perfil.GESTOR);
        fiscal = usuario(Perfil.GESTOR);
    }

    private Contrato contratoQueVenceEm(int dias) {
        return contrato(gestor, fiscal, dias, "Empresa Exemplo Ltda", "Sustentação");
    }

    private List<AvisoVencimentoEmitido> avisos() {
        return eventos.stream(AvisoVencimentoEmitido.class).toList();
    }

    private List<Long> destinatarios(AvisoVencimentoEmitido aviso) {
        return aviso.destinatarios().stream().map(Destinatario::id).toList();
    }

    @Test
    void avisaGestorEFiscalNoMarcoDe90Dias() {
        Contrato contrato = contratoQueVenceEm(90);

        assertThat(servico.processar()).isEqualTo(new AlertaVencimentoService.Resultado(1, 1));

        assertThat(avisos()).singleElement().satisfies(aviso -> {
            assertThat(aviso.contratoId()).isEqualTo(contrato.getId());
            assertThat(aviso.marco()).isEqualTo(90);
            assertThat(aviso.diasRestantes()).isEqualTo(90);
            assertThat(destinatarios(aviso)).containsExactly(gestor.getId(), fiscal.getId());
        });
        assertThat(alertas.findByContratoIdOrderByIdAsc(contrato.getId()))
                .singleElement()
                .extracting(AlertaVencimento::getMarco, AlertaVencimento::getDestinatarios)
                .containsExactly(90, 2);
    }

    @Test
    void naoAvisaForaDaJanela() {
        contratoQueVenceEm(91);
        contratoQueVenceEm(-1);

        assertThat(servico.processar().enviados()).isZero();
        assertThat(avisos()).isEmpty();
    }

    @Test
    void rodarDuasVezesNoMesmoDiaNaoDuplicaAvisos() {
        contratoQueVenceEm(60);

        servico.processar();
        assertThat(servico.processar().enviados()).isZero();
        assertThat(avisos()).hasSize(1);
    }

    @Test
    void enviaCadaMarcoUmaVezAoLongoDoTempo() {
        contratoQueVenceEm(90);

        // Simula a execução diária até a data de vencimento.
        for (int dia = 0; dia <= 90; dia++) {
            servico.processar();
            relogio.avancar(Duration.ofDays(1));
        }

        assertThat(avisos()).extracting(AvisoVencimentoEmitido::marco).containsExactly(90, 60, 30);
    }

    @Test
    void prorrogacaoPorAditivoReiniciaOCicloDeAvisos() {
        Contrato contrato = contratoQueVenceEm(30);
        servico.processar();

        var hoje = relogioDaAplicacao.hoje();
        aditivos.registrar(contrato.getId(), new AditivoForm("prazo", hoje, hoje.plusDays(85), null, null), gestor);

        assertThat(servico.processar().enviados()).isEqualTo(1);
        assertThat(alertas.findByContratoIdOrderByIdAsc(contrato.getId()))
                .extracting(AlertaVencimento::getMarco, AlertaVencimento::getDataFimReferencia)
                .containsExactly(
                        tuple(30, hoje.plusDays(30)),
                        tuple(90, hoje.plusDays(85)));
    }

    @Test
    void quandoGestorEFiscalSaoAMesmaPessoaEnviaUmUnicoEmail() {
        contrato(gestor, null, 30, "Empresa Exemplo Ltda", "Sustentação");

        servico.processar();

        assertThat(avisos()).singleElement().satisfies(aviso -> assertThat(destinatarios(aviso)).containsExactly(gestor.getId()));
    }

    @Test
    void fiscaisDesignadosEmVigorTambemRecebem() {
        Contrato contrato = contratoQueVenceEm(60);
        Usuario designado = usuario(Perfil.GESTOR);
        Usuario exFiscal = usuario(Perfil.GESTOR);
        designar(contrato, designado, TipoFiscal.TECNICO, null);
        designar(contrato, exFiscal, TipoFiscal.TECNICO, relogioDaAplicacao.hoje().minusDays(1));

        servico.processar();

        assertThat(avisos()).singleElement().satisfies(aviso ->
                assertThat(destinatarios(aviso)).containsExactly(gestor.getId(), fiscal.getId(), designado.getId()));
        assertThat(alertas.findByContratoIdOrderByIdAsc(contrato.getId()).getFirst().getDestinatarios()).isEqualTo(3);
    }
}
