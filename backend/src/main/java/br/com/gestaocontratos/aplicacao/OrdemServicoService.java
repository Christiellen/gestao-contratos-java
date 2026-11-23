package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Formularios.MedicaoForm;
import br.com.gestaocontratos.aplicacao.dto.Formularios.OrdemServicoForm;
import br.com.gestaocontratos.aplicacao.dto.Paginado;
import br.com.gestaocontratos.aplicacao.dto.Visoes.OrdemServicoVisao;
import br.com.gestaocontratos.dominio.CalculadoraGlosa;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.Formatos;
import br.com.gestaocontratos.dominio.RegraNegocioException;
import br.com.gestaocontratos.dominio.ResponsavelFase;
import br.com.gestaocontratos.dominio.TipoOrdemServico;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Empenho;
import br.com.gestaocontratos.dominio.modelo.Indicador;
import br.com.gestaocontratos.dominio.modelo.MedicaoIndicador;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import br.com.gestaocontratos.repositorio.EmpenhoRepositorio;
import br.com.gestaocontratos.repositorio.IndicadorRepositorio;
import br.com.gestaocontratos.repositorio.OrdemServicoRepositorio;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida da ordem de serviço: emissão, medições, avanço de fase,
 * devolução, cancelamento e registro no diário de eventos.
 */
@Service
@Transactional
public class OrdemServicoService {

    private final OrdemServicoRepositorio ordens;
    private final ContratoRepositorio contratos;
    private final EmpenhoRepositorio empenhos;
    private final IndicadorRepositorio indicadores;
    private final ResponsabilidadesOS responsabilidades;
    private final PoliticaAcesso politica;
    private final ConversorVisoes visoes;
    private final Relogio relogio;
    private final CalculadoraGlosa glosas = new CalculadoraGlosa();

    public OrdemServicoService(OrdemServicoRepositorio ordens, ContratoRepositorio contratos, EmpenhoRepositorio empenhos,
                               IndicadorRepositorio indicadores, ResponsabilidadesOS responsabilidades,
                               PoliticaAcesso politica, ConversorVisoes visoes, Relogio relogio) {
        this.ordens = ordens;
        this.contratos = contratos;
        this.empenhos = empenhos;
        this.indicadores = indicadores;
        this.responsabilidades = responsabilidades;
        this.politica = politica;
        this.visoes = visoes;
        this.relogio = relogio;
    }

    public record Filtros(String fase, Long contratoId, String busca, Integer pagina) {
    }

    @Transactional(readOnly = true)
    public Paginado<OrdemServicoVisao> listar(Filtros filtros, Usuario usuario) {
        Validacao v = new Validacao();
        String faseTexto = filtros.fase() == null || filtros.fase().isBlank() ? null : filtros.fase();
        FaseOrdemServico fase = v.opcao("fase", "fase", faseTexto, FaseOrdemServico.class);
        v.maximo("busca", "busca", filtros.busca(), 100);
        v.lancarSeHouverErros();

        Specification<OrdemServico> filtro = Specification.where(null);
        if (fase != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("fase"), fase));
        }
        if (filtros.contratoId() != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("contrato").get("id"), filtros.contratoId()));
        }
        if (filtros.busca() != null && !filtros.busca().isBlank()) {
            String termo = filtros.busca().trim();
            String padrao = "%" + termo.toLowerCase(Locale.ROOT) + "%";
            filtro = filtro.and((raiz, consulta, cb) -> cb.or(
                    cb.like(cb.lower(raiz.get("descricao")), padrao),
                    cb.like(raiz.get("numero"), "%" + termo + "%")));
        }
        int pagina = Math.max(1, filtros.pagina() == null ? 1 : filtros.pagina());
        var resultado = ordens.findAll(filtro, PageRequest.of(pagina - 1, 20, Sort.by(Sort.Direction.DESC, "id")));
        return Paginado.de(resultado, os -> visoes.ordemResumo(os, usuario));
    }

    @Transactional(readOnly = true)
    public OrdemServicoVisao detalhar(Long id, Usuario usuario) {
        return visoes.ordemCompleta(buscar(id), usuario);
    }

    public OrdemServicoVisao criar(OrdemServicoForm f, Usuario autor) {
        Validacao v = new Validacao();
        Contrato contrato = null;
        if (v.obrigatorio("contrato_id", "contrato", f.contratoId())) {
            contrato = contratos.findById(f.contratoId()).orElse(null);
            v.se(contrato == null, "contrato_id", "O contrato selecionado não é válido.");
        }
        TipoOrdemServico tipo = v.obrigatorio("tipo", "tipo", f.tipo()) ? v.opcao("tipo", "tipo", f.tipo(), TipoOrdemServico.class) : null;
        if (v.obrigatorio("descricao", "descrição", f.descricao())) {
            v.maximo("descricao", "descrição", f.descricao(), 500);
        }
        if (v.obrigatorio("valor_estimado", "valor estimado", f.valorEstimado())) {
            v.minimo("valor_estimado", "valor estimado", f.valorEstimado(), "0.01");
        }
        v.obrigatorio("prazo_entrega", "prazo de entrega", f.prazoEntrega());
        if (f.empenhoId() != null && f.contratoId() != null) {
            v.se(!empenhos.existsByIdAndContratoId(f.empenhoId(), f.contratoId()), "empenho_id",
                    "O empenho precisa ser do mesmo contrato da OS.");
        }
        v.lancarSeHouverErros();
        AcessoNegadoException.exigir(politica.podeAlterar(autor, contrato));

        // Trava o contrato para que duas emissões simultâneas não gerem o mesmo número.
        contratos.travar(contrato.getId());
        int ano = relogio.hoje().getYear();
        long sequencial = ordens.countByContratoIdAndNumeroEndingWith(contrato.getId(), "/" + ano) + 1;
        Empenho empenho = f.empenhoId() == null ? null : empenhos.getReferenceById(f.empenhoId());

        OrdemServico os = new OrdemServico(contrato, empenho, "%04d/%d".formatted(sequencial, ano), tipo, f.descricao(),
                f.valorEstimado(), f.prazoEntrega(), autor);
        os.registrarCriacao(autor, relogio.agora());
        return visoes.ordemCompleta(ordens.save(os), autor);
    }

    /** Registra (ou substitui) as medições dos indicadores e recalcula a glosa. */
    public OrdemServicoVisao registrarMedicoes(Long id, List<MedicaoForm> medicoes, Usuario autor) {
        validarMedicoes(medicoes);
        OrdemServico os = buscar(id);
        if (os.getFase() != FaseOrdemServico.FISCALIZACAO_TECNICA) {
            throw new RegraNegocioException("As medições só podem ser registradas na fase de fiscalização técnica.");
        }
        responsabilidades.exigir(os, Optional.of(ResponsavelFase.FISCAL_TECNICO), autor);

        Map<Long, Indicador> doContrato = indicadores.findByContratoIdOrderBySigla(os.getContrato().getId()).stream()
                .collect(Collectors.toMap(Indicador::getId, Function.identity()));
        for (MedicaoForm m : medicoes) {
            Indicador indicador = doContrato.get(m.indicadorId());
            if (indicador == null) {
                throw new RegraNegocioException("Indicador não pertence ao contrato desta OS.");
            }
            BigDecimal resultado = glosas.resultado(m.realizado(), m.previsto());
            os.registrarMedicao(indicador, m.realizado(), m.previsto(), resultado,
                    glosas.glosaDaFaixa(indicador.getFaixas(), resultado), m.justificativa(), autor);
        }

        BigDecimal percentual = glosas.percentualTotal(os.getMedicoes().stream().map(MedicaoIndicador::getPercentualGlosa).toList());
        os.aplicarGlosa(percentual, glosas.aplicar(os.getValorEstimado(), percentual));
        return visoes.ordemCompleta(os, autor);
    }

    public OrdemServicoVisao avancar(Long id, Usuario autor, String observacao) {
        validarTexto("observacao", "observação", observacao, false);
        OrdemServico os = buscar(id);
        FaseOrdemServico atual = os.getFase();
        FaseOrdemServico proxima = atual.proxima().orElseThrow(() -> new RegraNegocioException("Esta OS já está finalizada."));

        responsabilidades.exigir(os, atual.responsavel(), autor);
        validarPreCondicoes(os, proxima);

        if (atual == FaseOrdemServico.FISCALIZACAO_TECNICA && os.getValorFinal() == null) {
            // Contrato sem indicadores: o valor final é o estimado.
            os.aplicarGlosa(BigDecimal.ZERO, glosas.aplicar(os.getValorEstimado(), BigDecimal.ZERO));
        }
        os.mudarFase(proxima, "avanco", observacao, autor, relogio.agora());
        return visoes.ordemCompleta(os, autor);
    }

    /** O fiscal técnico devolve a entrega para a contratada corrigir. */
    public OrdemServicoVisao devolver(Long id, Usuario autor, String motivo) {
        validarTexto("motivo", "motivo", motivo, true);
        OrdemServico os = buscar(id);
        if (!os.getFase().podeDevolver()) {
            throw new RegraNegocioException("Só é possível devolver a OS durante a fiscalização técnica.");
        }
        responsabilidades.exigir(os, Optional.of(ResponsavelFase.FISCAL_TECNICO), autor);
        os.mudarFase(FaseOrdemServico.EM_EXECUCAO, "devolucao", motivo, autor, relogio.agora());
        return visoes.ordemCompleta(os, autor);
    }

    public OrdemServicoVisao cancelar(Long id, Usuario autor, String motivo) {
        validarTexto("motivo", "motivo", motivo, true);
        OrdemServico os = buscar(id);
        if (!os.getFase().podeCancelar()) {
            throw new RegraNegocioException(os.getFase().finalizada()
                    ? "Esta OS já está finalizada."
                    : "Depois da nota fiscal a OS não pode mais ser cancelada.");
        }
        responsabilidades.exigir(os, Optional.of(ResponsavelFase.GESTOR), autor);
        os.mudarFase(FaseOrdemServico.CANCELADA, "cancelamento", motivo, autor, relogio.agora());
        return visoes.ordemCompleta(os, autor);
    }

    public OrdemServicoVisao vincularEmpenho(Long id, Long empenhoId, Usuario autor) {
        OrdemServico os = buscar(id);
        AcessoNegadoException.exigir(politica.podeAlterar(autor, os.getContrato()));
        Validacao v = new Validacao();
        if (v.obrigatorio("empenho_id", "empenho", empenhoId)) {
            v.se(!empenhos.existsByIdAndContratoId(empenhoId, os.getContrato().getId()), "empenho_id",
                    "O empenho precisa ser do mesmo contrato da OS.");
        }
        v.lancarSeHouverErros();
        if (os.getFase().consomeEmpenho()) {
            throw new RegraNegocioException("O pagamento desta OS já foi autorizado.");
        }
        os.vincularEmpenho(empenhos.getReferenceById(empenhoId));
        return visoes.ordemCompleta(os, autor);
    }

    private void validarPreCondicoes(OrdemServico os, FaseOrdemServico proxima) {
        if (proxima == FaseOrdemServico.FISCALIZACAO_ADMINISTRATIVA) {
            Set<Long> medidos = new HashSet<>();
            os.getMedicoes().forEach(m -> medidos.add(m.getIndicador().getId()));
            List<String> faltando = indicadores.findByContratoIdOrderBySigla(os.getContrato().getId()).stream()
                    .filter(i -> !medidos.contains(i.getId()))
                    .map(Indicador::getSigla)
                    .toList();
            if (!faltando.isEmpty()) {
                throw new RegraNegocioException("Registre as medições dos indicadores antes de avançar: "
                        + String.join(", ", faltando) + ".");
            }
        }

        if (proxima == FaseOrdemServico.PAGAMENTO_AUTORIZADO) {
            Empenho empenho = os.getEmpenho();
            if (empenho == null) {
                throw new RegraNegocioException("Vincule uma nota de empenho antes de autorizar o pagamento.");
            }
            BigDecimal saldo = empenho.getValor().subtract(visoes.valorUtilizado(empenho, os.getId()));
            if (os.getValorFinal().compareTo(saldo) > 0) {
                throw new RegraNegocioException("Saldo insuficiente no empenho %s: disponível R$ %s, necessário R$ %s.".formatted(
                        empenho.getNumero(), Formatos.moeda(saldo), Formatos.moeda(os.getValorFinal())));
            }
        }
    }

    private static void validarMedicoes(List<MedicaoForm> medicoes) {
        Validacao v = new Validacao();
        if (v.obrigatorio("medicoes", "medições", medicoes)) {
            Set<Long> vistos = new HashSet<>();
            for (int i = 0; i < medicoes.size(); i++) {
                MedicaoForm m = medicoes.get(i);
                String campo = "medicoes." + i;
                if (m == null) {
                    v.erro(campo, "Medição inválida.");
                    continue;
                }
                if (v.obrigatorio(campo + ".indicador_id", "indicador", m.indicadorId())) {
                    v.se(!vistos.add(m.indicadorId()), campo + ".indicador_id", "O indicador foi informado mais de uma vez.");
                }
                if (v.obrigatorio(campo + ".realizado", "realizado", m.realizado())) {
                    v.minimo(campo + ".realizado", "realizado", m.realizado(), "0");
                }
                if (v.obrigatorio(campo + ".previsto", "previsto", m.previsto())) {
                    v.se(m.previsto().signum() <= 0, campo + ".previsto", "O campo previsto deve ser maior que zero.");
                }
                v.maximo(campo + ".justificativa", "justificativa", m.justificativa(), 500);
            }
        }
        v.lancarSeHouverErros();
    }

    private static void validarTexto(String campo, String rotulo, String texto, boolean obrigatorio) {
        Validacao v = new Validacao();
        if (!obrigatorio || v.obrigatorio(campo, rotulo, texto)) {
            v.maximo(campo, rotulo, texto, 500);
        }
        v.lancarSeHouverErros();
    }

    private OrdemServico buscar(Long id) {
        return ordens.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço"));
    }
}
