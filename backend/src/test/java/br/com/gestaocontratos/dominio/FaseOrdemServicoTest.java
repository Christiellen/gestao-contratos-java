package br.com.gestaocontratos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FaseOrdemServicoTest {

    @Test
    void fluxoTemNoveFasesDaEmissaoAoEncerramento() {
        List<FaseOrdemServico> percorridas = new ArrayList<>();
        Optional<FaseOrdemServico> fase = Optional.of(FaseOrdemServico.EM_PREPARACAO);
        while (fase.isPresent()) {
            percorridas.add(fase.get());
            fase = fase.get().proxima();
        }
        assertThat(percorridas).containsExactlyElementsOf(FaseOrdemServico.fluxo()).hasSize(9);
    }

    @Test
    void cadaFaseTemSeuResponsavel() {
        assertThat(FaseOrdemServico.EM_EXECUCAO.responsavel()).contains(ResponsavelFase.GESTOR);
        assertThat(FaseOrdemServico.FISCALIZACAO_TECNICA.responsavel()).contains(ResponsavelFase.FISCAL_TECNICO);
        assertThat(FaseOrdemServico.FISCALIZACAO_ADMINISTRATIVA.responsavel()).contains(ResponsavelFase.FISCAL_ADMINISTRATIVO);
        assertThat(FaseOrdemServico.ENCERRADA.responsavel()).isEmpty();
        assertThat(FaseOrdemServico.CANCELADA.proxima()).isEmpty();
    }

    @Test
    void cancelamentoSoAteORecebimentoDefinitivo() {
        assertThat(FaseOrdemServico.RECEBIMENTO_DEFINITIVO.podeCancelar()).isTrue();
        assertThat(FaseOrdemServico.NOTA_FISCAL.podeCancelar()).isFalse();
        assertThat(FaseOrdemServico.queConsomemEmpenho())
                .containsExactly(FaseOrdemServico.PAGAMENTO_AUTORIZADO, FaseOrdemServico.ENCERRADA);
    }

    @Test
    void valorGravadoEhOMesmoDaVersaoEmPhp() {
        assertThat(ComValor.deValor(FaseOrdemServico.class, "fiscalizacao_tecnica")).contains(FaseOrdemServico.FISCALIZACAO_TECNICA);
        assertThat(ComValor.deValor(FaseOrdemServico.class, "inexistente")).isEmpty();
    }
}
