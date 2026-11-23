package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.RelatorioService;
import br.com.gestaocontratos.aplicacao.dto.Visoes.TempoMedioVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ValoresPendentesVisao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/tempo-medio")
    @Operation(summary = "Tempo médio por transição de fase no período (padrão: últimos 12 meses)")
    public TempoMedioVisao tempoMedio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.tempoMedioPorTransicao(inicio, fim);
    }

    @GetMapping("/valores-pendentes")
    @Operation(summary = "Valor das OS ainda não pagas, por fase")
    public ValoresPendentesVisao valoresPendentes() {
        return relatorios.valoresPendentesPorFase();
    }
}
