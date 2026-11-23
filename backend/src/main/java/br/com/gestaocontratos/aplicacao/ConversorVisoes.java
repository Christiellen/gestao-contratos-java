package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Visoes.AditivoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ContratoResumo;
import br.com.gestaocontratos.aplicacao.dto.Visoes.ContratoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.EmpenhoResumo;
import br.com.gestaocontratos.aplicacao.dto.Visoes.EmpenhoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.EventoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.FiscalVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.IndicadorVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.MedicaoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.OrdemServicoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.Permissoes;
import br.com.gestaocontratos.aplicacao.dto.Visoes.PrepostoVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.UsuarioVisao;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.RegrasVencimento;
import br.com.gestaocontratos.dominio.ResponsavelFase;
import br.com.gestaocontratos.dominio.SituacaoContrato;
import br.com.gestaocontratos.dominio.modelo.Aditivo;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import br.com.gestaocontratos.dominio.modelo.Empenho;
import br.com.gestaocontratos.dominio.modelo.EventoOrdemServico;
import br.com.gestaocontratos.dominio.modelo.Indicador;
import br.com.gestaocontratos.dominio.modelo.MedicaoIndicador;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.dominio.modelo.Preposto;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.OrdemServicoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Converte as entidades nas respostas da API. Deve ser chamado dentro de uma transação. */
@Component
public class ConversorVisoes {

    private final RegrasVencimento vencimento = new RegrasVencimento();
    private final Relogio relogio;
    private final PoliticaAcesso politica;
    private final ResponsabilidadesOS responsabilidades;
    private final OrdemServicoRepositorio ordens;

    public ConversorVisoes(Relogio relogio, PoliticaAcesso politica, ResponsabilidadesOS responsabilidades,
                           OrdemServicoRepositorio ordens) {
        this.relogio = relogio;
        this.politica = politica;
        this.responsabilidades = responsabilidades;
        this.ordens = ordens;
    }

    public UsuarioVisao usuario(Usuario u) {
        if (u == null) {
            return null;
        }
        return new UsuarioVisao(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().valor(), u.getPerfil().rotulo());
    }

    /** Contrato com gestor e fiscal; sem as listas de cadastros. */
    public ContratoVisao contrato(Contrato c, Usuario quemPergunta) {
        return contrato(c, quemPergunta, false);
    }

    public ContratoVisao contratoCompleto(Contrato c, Usuario quemPergunta) {
        return contrato(c, quemPergunta, true);
    }

    private ContratoVisao contrato(Contrato c, Usuario quemPergunta, boolean completo) {
        LocalDate hoje = relogio.hoje();
        SituacaoContrato situacao = vencimento.situacao(c.getDataFim(), hoje);
        return new ContratoVisao(
                c.getId(), c.getNumero(), c.getObjeto(), c.getContratada(), c.getCnpj(), c.getValorGlobal(),
                c.getDataInicio(), c.getDataFim(), vencimento.diasParaVencer(c.getDataFim(), hoje),
                situacao.valor(), situacao.rotulo(), usuario(c.getGestor()), usuario(c.getFiscal()),
                completo ? c.getAditivos().stream().map(this::aditivo).toList() : null,
                completo ? c.getEmpenhos().stream().map(this::empenho).toList() : null,
                completo ? c.getIndicadores().stream().map(this::indicador).toList() : null,
                completo ? c.getDesignacoes().stream().map(this::fiscal).toList() : null,
                completo ? c.getPrepostos().stream().map(this::preposto).toList() : null,
                politica.podeAlterar(quemPergunta, c),
                politica.podeExcluir(quemPergunta));
    }

    public AditivoVisao aditivo(Aditivo a) {
        OffsetDateTime registradoEm = a.getCriadoEm() == null ? null : a.getCriadoEm().atZone(relogio.fuso()).toOffsetDateTime();
        return new AditivoVisao(a.getId(), a.getNumero(), a.getTipo().valor(), a.getDataAssinatura(), a.getDataFimAnterior(),
                a.getNovaDataFim(), a.getValorAcrescimo(), a.getDescricao(), registradoEm);
    }

    public EmpenhoVisao empenho(Empenho e) {
        BigDecimal utilizado = valorUtilizado(e, -1);
        return new EmpenhoVisao(e.getId(), e.getNumero(), e.getAno(), e.getValor(), utilizado, e.getValor().subtract(utilizado),
                e.getDescricao());
    }

    /** Valor do empenho já consumido por OS pagas, opcionalmente ignorando uma OS. */
    public BigDecimal valorUtilizado(Empenho e, long ignorarOrdemId) {
        BigDecimal soma = ordens.valorUtilizado(e.getId(), FaseOrdemServico.queConsomemEmpenho(), ignorarOrdemId);
        return soma == null ? BigDecimal.ZERO.setScale(2) : soma;
    }

    public IndicadorVisao indicador(Indicador i) {
        return new IndicadorVisao(i.getId(), i.getSigla(), i.getNome(), i.getMeta(), i.getFaixas());
    }

    public FiscalVisao fiscal(DesignacaoFiscal d) {
        return new FiscalVisao(d.getId(), d.getUsuario().getNome(), d.getPortaria(), d.getTipo().valor(), d.getTipo().rotulo(),
                d.getPapel().valor(), d.getDataInicio(), d.getDataFim());
    }

    public PrepostoVisao preposto(Preposto p) {
        return new PrepostoVisao(p.getId(), p.getNome(), p.getEmail(), p.getTelefone(), p.getPapel().valor(),
                p.ativoEm(relogio.hoje()), p.getDataInicio(), p.getDataFim());
    }

    /** Versão da listagem: sem medições, eventos e indicadores. */
    public OrdemServicoVisao ordemResumo(OrdemServico os, Usuario quemPergunta) {
        return ordem(os, quemPergunta, false);
    }

    public OrdemServicoVisao ordemCompleta(OrdemServico os, Usuario quemPergunta) {
        return ordem(os, quemPergunta, true);
    }

    private OrdemServicoVisao ordem(OrdemServico os, Usuario quemPergunta, boolean completa) {
        FaseOrdemServico fase = os.getFase();
        Optional<ResponsavelFase> responsavel = fase.responsavel();
        Optional<FaseOrdemServico> proxima = fase.proxima();
        boolean podeAvancar = responsabilidades.podeAgir(os, responsavel, quemPergunta);
        boolean podeCancelar = fase.podeCancelar()
                && responsabilidades.podeAgir(os, Optional.of(ResponsavelFase.GESTOR), quemPergunta);

        Contrato c = os.getContrato();
        ContratoResumo contrato = new ContratoResumo(c.getId(), c.getNumero(), c.getContratada(),
                completa ? c.getIndicadores().stream().map(this::indicador).toList() : null);
        Empenho e = os.getEmpenho();
        EmpenhoResumo empenho = e == null ? null
                : new EmpenhoResumo(e.getId(), e.getNumero(), e.getValor().subtract(valorUtilizado(e, -1)));

        return new OrdemServicoVisao(
                os.getId(), os.getNumero(), os.getTipo().valor(), os.getTipo().rotulo(), os.getDescricao(),
                os.getValorEstimado(), os.getPercentualGlosa(), os.getValorGlosa(), os.getValorFinal(), os.getPrazoEntrega(),
                fase.valor(), fase.rotulo(),
                proxima.map(FaseOrdemServico::valor).orElse(null), proxima.map(FaseOrdemServico::rotulo).orElse(null),
                responsavel.map(ResponsavelFase::rotulo).orElse(null),
                contrato, empenho,
                completa ? os.getMedicoes().stream().map(this::medicao).toList() : null,
                completa ? os.getEventos().stream().map(this::evento).toList() : null,
                new Permissoes(
                        podeAvancar && proxima.isPresent(),
                        fase == FaseOrdemServico.FISCALIZACAO_TECNICA && podeAvancar,
                        fase.podeDevolver() && podeAvancar,
                        podeCancelar));
    }

    private MedicaoVisao medicao(MedicaoIndicador m) {
        return new MedicaoVisao(m.getIndicador().getId(), m.getIndicador().getSigla(), m.getRealizado(), m.getPrevisto(),
                m.getResultado(), m.getPercentualGlosa(), m.getJustificativa());
    }

    private EventoVisao evento(EventoOrdemServico e) {
        return new EventoVisao(e.getAcao(),
                e.getFaseAnterior() == null ? null : e.getFaseAnterior().rotulo(),
                e.getFaseNova().rotulo(), e.getObservacao(), e.getUsuario().getNome(),
                e.getOcorridoEm().atZone(relogio.fuso()).toOffsetDateTime());
    }

    public List<UsuarioVisao> usuarios(List<Usuario> usuarios) {
        return usuarios.stream().map(this::usuario).toList();
    }
}
