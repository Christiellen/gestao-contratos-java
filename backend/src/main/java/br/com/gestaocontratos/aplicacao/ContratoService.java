package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Formularios.ContratoForm;
import br.com.gestaocontratos.aplicacao.dto.Paginado;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ContratoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.PainelVisao;
import br.com.gestaocontratos.dominio.Cnpj;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.SituacaoContrato;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import br.com.gestaocontratos.repositorio.FiltrosContrato;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContratoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999999.99");

    private final ContratoRepositorio contratos;
    private final UsuarioRepositorio usuarios;
    private final PoliticaAcesso politica;
    private final ConversorVisoes visoes;
    private final Relogio relogio;

    public ContratoService(ContratoRepositorio contratos, UsuarioRepositorio usuarios, PoliticaAcesso politica,
                           ConversorVisoes visoes, Relogio relogio) {
        this.contratos = contratos;
        this.usuarios = usuarios;
        this.politica = politica;
        this.visoes = visoes;
        this.relogio = relogio;
    }

    public record Filtros(String situacao, String busca, Long gestorId, Integer pagina, Integer porPagina) {
    }

    @Transactional(readOnly = true)
    public Paginado<ContratoVisao> listar(Filtros filtros, Usuario usuario) {
        Validacao v = new Validacao();
        SituacaoContrato situacao = v.opcao("situacao", "situação", vazioParaNulo(filtros.situacao()), SituacaoContrato.class);
        v.maximo("busca", "busca", filtros.busca(), 100);
        int porPagina = Objects.requireNonNullElse(filtros.porPagina(), 15);
        v.se(porPagina < 1 || porPagina > 100, "por_pagina", "O campo por página deve estar entre 1 e 100.");
        v.lancarSeHouverErros();

        Specification<Contrato> filtro = Specification.where(null);
        if (situacao != null) {
            filtro = filtro.and(FiltrosContrato.situacao(situacao, relogio.hoje()));
        }
        if (filtros.busca() != null && !filtros.busca().isBlank()) {
            filtro = filtro.and(FiltrosContrato.busca(filtros.busca()));
        }
        if (filtros.gestorId() != null) {
            filtro = filtro.and(FiltrosContrato.gestor(filtros.gestorId()));
        }
        int pagina = Math.max(1, Objects.requireNonNullElse(filtros.pagina(), 1));
        var resultado = contratos.findAll(filtro, PageRequest.of(pagina - 1, porPagina, Sort.by("dataFim", "id")));
        return Paginado.de(resultado, c -> visoes.contrato(c, usuario));
    }

    @Transactional
    public ContratoVisao criar(ContratoForm form, Usuario usuario) {
        AcessoNegadoException.exigir(politica.podeCriarContrato(usuario));
        Dados dados = validar(form, 0L);
        Contrato contrato = contratos.save(new Contrato(dados.numero, form.objeto(), form.contratada(), dados.cnpj,
                form.valorGlobal(), form.dataInicio(), form.dataFim(), dados.gestor, dados.fiscal));
        return visoes.contratoCompleto(contrato, usuario);
    }

    @Transactional(readOnly = true)
    public ContratoVisao detalhar(Long id, Usuario usuario) {
        return visoes.contratoCompleto(buscar(id), usuario);
    }

    @Transactional
    public ContratoVisao atualizar(Long id, ContratoForm form, Usuario usuario) {
        Contrato contrato = buscar(id);
        AcessoNegadoException.exigir(politica.podeAlterar(usuario, contrato));
        Dados dados = validar(form, id);
        contrato.alterar(dados.numero, form.objeto(), form.contratada(), dados.cnpj, form.valorGlobal(),
                form.dataInicio(), form.dataFim(), dados.gestor, dados.fiscal);
        return visoes.contrato(contrato, usuario);
    }

    @Transactional
    public void excluir(Long id, Usuario usuario) {
        Contrato contrato = buscar(id);
        AcessoNegadoException.exigir(politica.podeExcluir(usuario));
        contratos.delete(contrato);
    }

    /** Resumo da tela inicial: totais por situação e próximos vencimentos. */
    @Transactional(readOnly = true)
    public PainelVisao painel(Usuario usuario) {
        LocalDate hoje = relogio.hoje();
        Map<String, Long> totais = new LinkedHashMap<>();
        for (SituacaoContrato situacao : SituacaoContrato.values()) {
            totais.put(situacao.valor(), contratos.count(FiltrosContrato.situacao(situacao, hoje)));
        }
        List<ContratoVisao> proximos = contratos
                .findAll(FiltrosContrato.situacao(SituacaoContrato.A_VENCER, hoje), PageRequest.of(0, 10, Sort.by("dataFim", "id")))
                .map(c -> visoes.contrato(c, usuario))
                .getContent();
        BigDecimal valorVigente = Objects.requireNonNullElse(contratos.somarValorVigente(hoje), BigDecimal.ZERO);
        return new PainelVisao(totais, valorVigente, proximos);
    }

    Contrato buscar(Long id) {
        return contratos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Contrato"));
    }

    private record Dados(String numero, String cnpj, Usuario gestor, Usuario fiscal) {
    }

    private Dados validar(ContratoForm f, long ignorarId) {
        Validacao v = new Validacao();
        String numero = f.numero() == null ? null : f.numero().trim();
        String cnpj = f.cnpj() == null ? null : Cnpj.somenteDigitos(f.cnpj());

        if (v.obrigatorio("numero", "número do contrato", numero)) {
            v.maximo("numero", "número do contrato", numero, 30);
            v.se(contratos.numeroEmUso(numero, ignorarId), "numero", "Já existe um contrato com este número.");
        }
        if (v.obrigatorio("objeto", "objeto", f.objeto())) {
            v.maximo("objeto", "objeto", f.objeto(), 500);
        }
        if (v.obrigatorio("contratada", "empresa contratada", f.contratada())) {
            v.maximo("contratada", "empresa contratada", f.contratada(), 200);
        }
        if (v.obrigatorio("cnpj", "CNPJ", f.cnpj())) {
            v.se(!Cnpj.valido(cnpj), "cnpj", "O CNPJ informado não é um CNPJ válido.");
        }
        if (v.obrigatorio("valor_global", "valor global", f.valorGlobal())) {
            v.entre("valor_global", "valor global", f.valorGlobal(), "0.01", VALOR_MAXIMO.toPlainString());
        }
        v.obrigatorio("data_inicio", "data de início", f.dataInicio());
        if (v.obrigatorio("data_fim", "data de fim", f.dataFim()) && f.dataInicio() != null) {
            v.se(!f.dataFim().isAfter(f.dataInicio()), "data_fim", "A data de fim deve ser posterior à data de início.");
        }

        Usuario gestor = null;
        if (v.obrigatorio("gestor_id", "gestor", f.gestorId())) {
            gestor = usuarios.findById(f.gestorId())
                    .filter(u -> u.getPerfil() == Perfil.GESTOR || u.getPerfil() == Perfil.ADMINISTRADOR)
                    .orElse(null);
            v.se(gestor == null, "gestor_id", "O gestor selecionado não é válido.");
        }
        Usuario fiscal = null;
        if (f.fiscalId() != null) {
            v.se(f.fiscalId().equals(f.gestorId()), "fiscal_id", "O fiscal deve ser diferente do gestor.");
            fiscal = usuarios.findById(f.fiscalId()).orElse(null);
            v.se(fiscal == null, "fiscal_id", "O fiscal selecionado não é válido.");
        }
        v.lancarSeHouverErros();
        return new Dados(numero, cnpj, gestor, fiscal);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
