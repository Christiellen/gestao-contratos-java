package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Visoes.PendenciaFaseVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.TempoMedioVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.TransicaoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ValoresPendentesVisao;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.Formatos;
import br.com.gestaocontratos.dominio.modelo.EventoOrdemServico;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.repositorio.EventoOrdemServicoRepositorio;
import br.com.gestaocontratos.repositorio.OrdemServicoRepositorio;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final EventoOrdemServicoRepositorio eventos;
    private final OrdemServicoRepositorio ordens;
    private final Relogio relogio;

    public RelatorioService(EventoOrdemServicoRepositorio eventos, OrdemServicoRepositorio ordens, Relogio relogio) {
        this.eventos = eventos;
        this.ordens = ordens;
        this.relogio = relogio;
    }

    private record Chave(FaseOrdemServico de, FaseOrdemServico para) {
    }

    private static final class Soma {
        long totalSegundos;
        int quantidade;
    }

    /**
     * Tempo médio que as OS levam em cada transição de fase, no período.
     * O tempo de uma transição é o intervalo entre a entrada na fase de origem e a entrada na fase de destino.
     */
    public TempoMedioVisao tempoMedioPorTransicao(LocalDate inicio, LocalDate fim) {
        LocalDate de = Objects.requireNonNullElse(inicio, relogio.hoje().minusYears(1));
        LocalDate ate = Objects.requireNonNullElse(fim, relogio.hoje());
        if (ate.isBefore(de)) {
            throw ValidacaoException.doCampo("fim", "A data final deve ser igual ou posterior à inicial.");
        }
        Instant desde = de.atStartOfDay(relogio.fuso()).toInstant();
        Instant until = ate.plusDays(1).atStartOfDay(relogio.fuso()).toInstant().minusNanos(1_000);

        Map<Chave, Soma> somas = new HashMap<>();
        Map<Long, Instant> entradaNaFase = new HashMap<>();
        for (EventoOrdemServico e : eventos.doPeriodo(desde, until)) {
            Long os = e.getOrdemServicoId();
            Instant entrada = entradaNaFase.get(os);
            if (e.getFaseAnterior() != null && entrada != null) {
                Soma soma = somas.computeIfAbsent(new Chave(e.getFaseAnterior(), e.getFaseNova()), c -> new Soma());
                soma.totalSegundos += Duration.between(entrada, e.getOcorridoEm()).toSeconds();
                soma.quantidade++;
            }
            entradaNaFase.put(os, e.getOcorridoEm());
        }

        List<TransicaoVisao> transicoes = new ArrayList<>();
        somas.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Chave, Soma> x) -> x.getKey().de().ordinal())
                        .thenComparing(x -> x.getKey().para().ordinal()))
                .forEach(x -> {
                    Chave chave = x.getKey();
                    long media = x.getValue().totalSegundos / x.getValue().quantidade;
                    transicoes.add(new TransicaoVisao(chave.de().valor(), chave.para().valor(),
                            chave.de().rotulo() + " → " + chave.para().rotulo(),
                            x.getValue().quantidade, media, Formatos.duracao(media)));
                });
        return new TempoMedioVisao(de, ate, transicoes);
    }

    /** Valor das OS ainda não pagas, por fase (valor final se já medido, senão o estimado). */
    public ValoresPendentesVisao valoresPendentesPorFase() {
        List<PendenciaFaseVisao> linhas = FaseOrdemServico.fluxo().stream()
                .filter(f -> !f.finalizada() && f != FaseOrdemServico.PAGAMENTO_AUTORIZADO)
                .map(fase -> {
                    List<OrdemServico> daFase = ordens.findByFase(fase);
                    BigDecimal valor = daFase.stream()
                            .map(o -> o.getValorFinal() != null ? o.getValorFinal() : o.getValorEstimado())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new PendenciaFaseVisao(fase.valor(), fase.rotulo(), daFase.size(), valor);
                })
                .toList();
        BigDecimal total = linhas.stream().map(PendenciaFaseVisao::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ValoresPendentesVisao(linhas, total);
    }
}
