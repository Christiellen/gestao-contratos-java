package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.AditivoService;
import br.com.gestaocontratos.aplicacao.CadastroContratoService;
import br.com.gestaocontratos.aplicacao.ContratoService;
import br.com.gestaocontratos.aplicacao.dto.Dados;
import br.com.gestaocontratos.aplicacao.dto.Formularios.AditivoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.ContratoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.EmpenhoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.FiscalForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.IndicadorForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.PrepostoForm;
import br.com.gestaocontratos.aplicacao.dto.Paginado;
import br.com.gestaocontratos.aplicacao.dto.Visoes.AditivoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ContratoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.EmpenhoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.FiscalVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.IndicadorVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.PrepostoVisao;
import br.com.gestaocontratos.seguranca.SessaoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/contratos")
@Tag(name = "Contratos")
public class ContratoController {

    private final ContratoService contratos;
    private final AditivoService aditivos;
    private final CadastroContratoService cadastros;

    public ContratoController(ContratoService contratos, AditivoService aditivos, CadastroContratoService cadastros) {
        this.contratos = contratos;
        this.aditivos = aditivos;
        this.cadastros = cadastros;
    }

    @GetMapping
    @Operation(summary = "Lista paginada, com filtro por situação (vigente, a_vencer, vencido), busca e gestor")
    public Paginado<ContratoVisao> listar(
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String busca,
            @RequestParam(name = "gestor_id", required = false) Long gestorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "por_pagina", required = false) Integer porPagina,
            @AuthenticationPrincipal SessaoUsuario sessao) {
        return contratos.listar(new ContratoService.Filtros(situacao, busca, gestorId, page, porPagina), sessao.usuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um contrato (administrador ou gestor)")
    public Dados<ContratoVisao> criar(@RequestBody ContratoForm form, @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(contratos.criar(form, sessao.usuario()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe com aditivos, empenhos, indicadores, fiscais e prepostos")
    public Dados<ContratoVisao> detalhar(@PathVariable Long id, @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(contratos.detalhar(id, sessao.usuario()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o contrato (administrador ou o gestor do contrato)")
    public Dados<ContratoVisao> atualizar(@PathVariable Long id, @RequestBody ContratoForm form,
                                          @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(contratos.atualizar(id, form, sessao.usuario()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclusão lógica (somente administrador)")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal SessaoUsuario sessao) {
        contratos.excluir(id, sessao.usuario());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aditivos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra termo aditivo de prazo, de valor ou de ambos")
    public Dados<AditivoVisao> registrarAditivo(@PathVariable Long id, @RequestBody AditivoForm form,
                                                @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(aditivos.registrar(id, form, sessao.usuario()));
    }

    @PostMapping("/{id}/empenhos")
    @ResponseStatus(HttpStatus.CREATED)
    public Dados<EmpenhoVisao> criarEmpenho(@PathVariable Long id, @RequestBody EmpenhoForm form,
                                            @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(cadastros.criarEmpenho(id, form, sessao.usuario()));
    }

    @PostMapping("/{id}/indicadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Dados<IndicadorVisao> criarIndicador(@PathVariable Long id, @RequestBody IndicadorForm form,
                                                @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(cadastros.criarIndicador(id, form, sessao.usuario()));
    }

    @PostMapping("/{id}/fiscais")
    @ResponseStatus(HttpStatus.CREATED)
    public Dados<FiscalVisao> criarFiscal(@PathVariable Long id, @RequestBody FiscalForm form,
                                          @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(cadastros.criarFiscal(id, form, sessao.usuario()));
    }

    @PostMapping("/{id}/prepostos")
    @ResponseStatus(HttpStatus.CREATED)
    public Dados<PrepostoVisao> criarPreposto(@PathVariable Long id, @RequestBody PrepostoForm form,
                                              @AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(cadastros.criarPreposto(id, form, sessao.usuario()));
    }
}
