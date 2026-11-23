package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.OrdemServicoService;
import br.com.gestaocontratos.aplicacao.dto.Dados;
import br.com.gestaocontratos.aplicacao.dto.Formularios.MedicoesForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.MotivoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.ObservacaoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.OrdemServicoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.VinculoEmpenhoForm;
import br.com.gestaocontratos.aplicacao.dto.Paginado;
import br.com.gestaocontratos.aplicacao.dto.Visoes.OrdemServicoVisao;
import br.com.gestaocontratos.seguranca.SessaoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-servico")
@Tag(name = "Ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoService servico;

    public OrdemServicoController(OrdemServicoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public Paginado<OrdemServicoVisao> listar(
            @RequestParam(required = false) String fase,
            @RequestParam(name = "contrato_id", required = false) Long contratoId,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer page,
            @AuthenticationPrincipal SessaoUsuario sessao) {
        return servico.listar(new OrdemServicoService.Filtros(fase, contratoId, busca, page), sessao.usuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emite OS com numeração sequencial por ano (0001/2026)")
    public Dados<OrdemServicoVisao> criar(@RequestBody OrdemServicoForm form, @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.criar(form, sessao.usuario()));
    }

    @GetMapping("/{id}")
    public Dados<OrdemServicoVisao> detalhar(@PathVariable Long id, @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.detalhar(id, sessao.usuario()));
    }

    @PutMapping("/{id}/medicoes")
    @Operation(summary = "Registra as medições dos indicadores e recalcula a glosa (fiscal técnico)")
    public Dados<OrdemServicoVisao> medicoes(@PathVariable Long id, @RequestBody MedicoesForm form,
                                             @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.registrarMedicoes(id, form.medicoes(), sessao.usuario()));
    }

    @PostMapping("/{id}/avancar")
    @Operation(summary = "Avança para a próxima fase, se o usuário for o responsável pela fase atual")
    public Dados<OrdemServicoVisao> avancar(@PathVariable Long id, @RequestBody(required = false) ObservacaoForm form,
                                            @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.avancar(id, sessao.usuario(), form == null ? null : form.observacao()));
    }

    @PostMapping("/{id}/devolver")
    public Dados<OrdemServicoVisao> devolver(@PathVariable Long id, @RequestBody MotivoForm form,
                                             @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.devolver(id, sessao.usuario(), form.motivo()));
    }

    @PostMapping("/{id}/cancelar")
    public Dados<OrdemServicoVisao> cancelar(@PathVariable Long id, @RequestBody MotivoForm form,
                                             @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.cancelar(id, sessao.usuario(), form.motivo()));
    }

    @PutMapping("/{id}/empenho")
    public Dados<OrdemServicoVisao> vincularEmpenho(@PathVariable Long id, @RequestBody VinculoEmpenhoForm form,
                                                    @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(servico.vincularEmpenho(id, form.empenhoId(), sessao.usuario()));
    }
}
