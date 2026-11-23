package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Formularios.AditivoForm;
import br.com.gestaocontratos.aplicacao.dto.Visoes.AditivoVisao;
import br.com.gestaocontratos.dominio.Formatos;
import br.com.gestaocontratos.dominio.TipoAditivo;
import br.com.gestaocontratos.dominio.modelo.Aditivo;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.AditivoRepositorio;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra termos aditivos e aplica os efeitos no contrato (nova vigência e/ou novo valor). */
@Service
public class AditivoService {

    private final ContratoRepositorio contratos;
    private final AditivoRepositorio aditivos;
    private final PoliticaAcesso politica;
    private final ConversorVisoes visoes;

    public AditivoService(ContratoRepositorio contratos, AditivoRepositorio aditivos, PoliticaAcesso politica,
                          ConversorVisoes visoes) {
        this.contratos = contratos;
        this.aditivos = aditivos;
        this.politica = politica;
        this.visoes = visoes;
    }

    @Transactional
    public AditivoVisao registrar(Long contratoId, AditivoForm f, Usuario autor) {
        // Trava o contrato: dois aditivos simultâneos não recebem o mesmo número nem perdem um ao outro.
        Contrato contrato = contratos.travar(contratoId).orElseThrow(() -> new RecursoNaoEncontradoException("Contrato"));
        AcessoNegadoException.exigir(politica.podeAlterar(autor, contrato));
        TipoAditivo tipo = validar(f, contrato);

        Aditivo aditivo = aditivos.save(new Aditivo(
                contrato,
                aditivos.ultimoNumero(contratoId) + 1,
                tipo,
                f.dataAssinatura(),
                tipo.alteraPrazo() ? contrato.getDataFim() : null,
                tipo.alteraPrazo() ? f.novaDataFim() : null,
                tipo.alteraValor() ? f.valorAcrescimo() : null,
                f.descricao(),
                autor));

        if (tipo.alteraPrazo()) {
            contrato.prorrogarAte(f.novaDataFim());
        }
        if (tipo.alteraValor()) {
            contrato.somarAoValorGlobal(f.valorAcrescimo());
        }
        return visoes.aditivo(aditivo);
    }

    private static TipoAditivo validar(AditivoForm f, Contrato contrato) {
        Validacao v = new Validacao();
        TipoAditivo tipo = v.obrigatorio("tipo", "tipo", f.tipo()) ? v.opcao("tipo", "tipo", f.tipo(), TipoAditivo.class) : null;
        v.obrigatorio("data_assinatura", "data de assinatura", f.dataAssinatura());
        v.maximo("descricao", "descrição", f.descricao(), 500);

        if (tipo != null && tipo.alteraPrazo() && v.obrigatorio("nova_data_fim", "nova data de fim", f.novaDataFim())) {
            v.se(!f.novaDataFim().isAfter(contrato.getDataFim()), "nova_data_fim",
                    "A nova data de fim deve ser posterior à vigência atual (" + contrato.getDataFim().format(Formatos.DATA) + ").");
        }
        if (tipo != null && tipo.alteraValor() && v.obrigatorio("valor_acrescimo", "valor do acréscimo", f.valorAcrescimo())) {
            v.se(f.valorAcrescimo().signum() == 0, "valor_acrescimo", "O valor do acréscimo não pode ser zero.");
            v.se(contrato.getValorGlobal().add(f.valorAcrescimo()).signum() <= 0, "valor_acrescimo",
                    "A supressão não pode zerar o valor global do contrato.");
        }
        v.lancarSeHouverErros();
        return tipo;
    }
}
