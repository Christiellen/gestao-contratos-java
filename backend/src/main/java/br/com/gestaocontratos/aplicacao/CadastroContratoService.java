package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Formularios.EmpenhoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.FaixaForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.FiscalForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.IndicadorForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.PrepostoForm;
import br.com.gestaocontratos.aplicacao.dto.Visoes.EmpenhoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.FiscalVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.IndicadorVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.PrepostoVisao;
import br.com.gestaocontratos.dominio.Faixa;
import br.com.gestaocontratos.dominio.Papel;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import br.com.gestaocontratos.dominio.modelo.Empenho;
import br.com.gestaocontratos.dominio.modelo.Indicador;
import br.com.gestaocontratos.dominio.modelo.Preposto;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.DesignacaoFiscalRepositorio;
import br.com.gestaocontratos.repositorio.EmpenhoRepositorio;
import br.com.gestaocontratos.repositorio.IndicadorRepositorio;
import br.com.gestaocontratos.repositorio.PrepostoRepositorio;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastros vinculados ao contrato: empenhos, indicadores, fiscais e prepostos. */
@Service
@Transactional
public class CadastroContratoService {

    private final ContratoService contratoService;
    private final PoliticaAcesso politica;
    private final EmpenhoRepositorio empenhos;
    private final IndicadorRepositorio indicadores;
    private final DesignacaoFiscalRepositorio designacoes;
    private final PrepostoRepositorio prepostos;
    private final UsuarioRepositorio usuarios;
    private final ConversorVisoes visoes;

    public CadastroContratoService(ContratoService contratoService, PoliticaAcesso politica, EmpenhoRepositorio empenhos,
                                   IndicadorRepositorio indicadores, DesignacaoFiscalRepositorio designacoes,
                                   PrepostoRepositorio prepostos, UsuarioRepositorio usuarios, ConversorVisoes visoes) {
        this.contratoService = contratoService;
        this.politica = politica;
        this.empenhos = empenhos;
        this.indicadores = indicadores;
        this.designacoes = designacoes;
        this.prepostos = prepostos;
        this.usuarios = usuarios;
        this.visoes = visoes;
    }

    public EmpenhoVisao criarEmpenho(Long contratoId, EmpenhoForm f, Usuario usuario) {
        Contrato contrato = contratoQuePodeAlterar(contratoId, usuario);
        Validacao v = new Validacao();
        if (v.obrigatorio("numero", "número", f.numero())) {
            v.maximo("numero", "número", f.numero(), 20);
            v.se(empenhos.existsByContratoIdAndNumero(contratoId, f.numero()), "numero",
                    "Já existe um empenho com este número no contrato.");
        }
        if (v.obrigatorio("ano", "ano", f.ano())) {
            v.se(f.ano() < 2000 || f.ano() > 2100, "ano", "O campo ano deve estar entre 2000 e 2100.");
        }
        if (v.obrigatorio("valor", "valor", f.valor())) {
            v.minimo("valor", "valor", f.valor(), "0.01");
        }
        v.maximo("descricao", "descrição", f.descricao(), 300);
        v.lancarSeHouverErros();

        Empenho empenho = empenhos.save(new Empenho(contrato, f.numero(), f.ano(), f.valor(), f.descricao()));
        return visoes.empenho(empenho);
    }

    public IndicadorVisao criarIndicador(Long contratoId, IndicadorForm f, Usuario usuario) {
        Contrato contrato = contratoQuePodeAlterar(contratoId, usuario);
        Validacao v = new Validacao();
        if (v.obrigatorio("sigla", "sigla", f.sigla())) {
            v.maximo("sigla", "sigla", f.sigla(), 10);
            v.se(indicadores.existsByContratoIdAndSiglaIgnoreCase(contratoId, f.sigla()), "sigla",
                    "Já existe um indicador com esta sigla no contrato.");
        }
        if (v.obrigatorio("nome", "nome", f.nome())) {
            v.maximo("nome", "nome", f.nome(), 150);
        }
        v.maximo("descricao", "descrição", f.descricao(), 500);
        if (v.obrigatorio("meta", "meta", f.meta())) {
            v.entre("meta", "meta", f.meta(), "0", "100");
        }
        if (v.obrigatorio("faixas", "faixas", f.faixas())) {
            Set<BigDecimal> minimos = new HashSet<>();
            for (int i = 0; i < f.faixas().size(); i++) {
                FaixaForm faixa = f.faixas().get(i);
                String campo = "faixas." + i;
                if (faixa == null) {
                    v.erro(campo + ".minimo", "Informe o mínimo da faixa.");
                    continue;
                }
                if (v.obrigatorio(campo + ".minimo", "mínimo", faixa.minimo())) {
                    v.entre(campo + ".minimo", "mínimo", faixa.minimo(), "0", "100");
                    v.se(!minimos.add(faixa.minimo().stripTrailingZeros()), campo + ".minimo", "Há duas faixas com o mesmo mínimo.");
                }
                if (v.obrigatorio(campo + ".glosa", "glosa", faixa.glosa())) {
                    v.entre(campo + ".glosa", "glosa", faixa.glosa(), "0", "100");
                }
            }
        }
        v.lancarSeHouverErros();

        // Guarda as faixas do maior mínimo para o menor, a ordem em que são avaliadas.
        List<Faixa> faixas = f.faixas().stream()
                .map(x -> new Faixa(x.minimo(), x.glosa()))
                .sorted(Comparator.comparing(Faixa::minimo).reversed())
                .toList();
        Indicador indicador = indicadores.save(new Indicador(contrato, f.sigla(), f.nome(), f.descricao(), f.meta(), faixas));
        return visoes.indicador(indicador);
    }

    public FiscalVisao criarFiscal(Long contratoId, FiscalForm f, Usuario usuario) {
        Contrato contrato = contratoQuePodeAlterar(contratoId, usuario);
        Validacao v = new Validacao();
        Usuario fiscal = null;
        if (v.obrigatorio("user_id", "servidor", f.userId())) {
            fiscal = usuarios.findById(f.userId()).orElse(null);
            v.se(fiscal == null, "user_id", "O servidor selecionado não é válido.");
        }
        if (v.obrigatorio("portaria", "portaria", f.portaria())) {
            v.maximo("portaria", "portaria", f.portaria(), 60);
        }
        TipoFiscal tipo = v.obrigatorio("tipo", "tipo", f.tipo()) ? v.opcao("tipo", "tipo", f.tipo(), TipoFiscal.class) : null;
        Papel papel = v.obrigatorio("papel", "papel", f.papel()) ? v.opcao("papel", "papel", f.papel(), Papel.class) : null;
        validarPeriodo(v, f.dataInicio(), f.dataFim());
        v.lancarSeHouverErros();

        DesignacaoFiscal designacao = designacoes.save(
                new DesignacaoFiscal(contrato, fiscal, f.portaria(), tipo, papel, f.dataInicio(), f.dataFim()));
        return visoes.fiscal(designacao);
    }

    public PrepostoVisao criarPreposto(Long contratoId, PrepostoForm f, Usuario usuario) {
        Contrato contrato = contratoQuePodeAlterar(contratoId, usuario);
        Validacao v = new Validacao();
        if (v.obrigatorio("nome", "nome", f.nome())) {
            v.maximo("nome", "nome", f.nome(), 150);
        }
        if (v.obrigatorio("email", "e-mail", f.email())) {
            v.maximo("email", "e-mail", f.email(), 150);
            v.email("email", f.email());
        }
        v.maximo("telefone", "telefone", f.telefone(), 20);
        Papel papel = v.obrigatorio("papel", "papel", f.papel()) ? v.opcao("papel", "papel", f.papel(), Papel.class) : null;
        validarPeriodo(v, f.dataInicio(), f.dataFim());
        v.lancarSeHouverErros();

        Preposto preposto = prepostos.save(
                new Preposto(contrato, f.nome(), f.email(), f.telefone(), papel, f.dataInicio(), f.dataFim()));
        return visoes.preposto(preposto);
    }

    private static void validarPeriodo(Validacao v, LocalDate inicio, LocalDate fim) {
        v.obrigatorio("data_inicio", "data de início", inicio);
        if (inicio != null && fim != null) {
            v.se(fim.isBefore(inicio), "data_fim", "A data de fim deve ser igual ou posterior à data de início.");
        }
    }

    private Contrato contratoQuePodeAlterar(Long contratoId, Usuario usuario) {
        Contrato contrato = contratoService.buscar(contratoId);
        AcessoNegadoException.exigir(politica.podeAlterar(usuario, contrato));
        return contrato;
    }
}
