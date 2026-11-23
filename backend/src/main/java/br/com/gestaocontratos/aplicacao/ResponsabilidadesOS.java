package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.dominio.RegraNegocioException;
import br.com.gestaocontratos.dominio.ResponsavelFase;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.DesignacaoFiscalRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Decide se o usuário pode agir numa fase da OS: o gestor do contrato ou o fiscal
 * designado e em vigor na data de hoje. Usado nas regras e para mostrar os botões no front-end.
 */
@Component
public class ResponsabilidadesOS {

    private final DesignacaoFiscalRepositorio designacoes;
    private final Relogio relogio;

    public ResponsabilidadesOS(DesignacaoFiscalRepositorio designacoes, Relogio relogio) {
        this.designacoes = designacoes;
        this.relogio = relogio;
    }

    public boolean podeAgir(OrdemServico os, Optional<ResponsavelFase> responsavel, Usuario usuario) {
        if (responsavel.isEmpty()) {
            return false;
        }
        if (usuario.isAdministrador()) {
            return true;
        }
        Long contratoId = os.getContrato().getId();
        return switch (responsavel.get()) {
            case GESTOR -> os.getContrato().geridoPor(usuario);
            case FISCAL_TECNICO -> designacoes.contarFiscalEmVigor(contratoId, usuario.getId(),
                    List.of(TipoFiscal.TECNICO, TipoFiscal.REQUISITANTE), relogio.hoje()) > 0;
            case FISCAL_ADMINISTRATIVO -> designacoes.contarFiscalEmVigor(contratoId, usuario.getId(),
                    List.of(TipoFiscal.ADMINISTRATIVO), relogio.hoje()) > 0;
        };
    }

    public void exigir(OrdemServico os, Optional<ResponsavelFase> responsavel, Usuario usuario) {
        if (!podeAgir(os, responsavel, usuario)) {
            String quem = responsavel.map(ResponsavelFase::rotulo).orElse("ninguém");
            throw new RegraNegocioException("Esta etapa cabe a: " + quem + ".");
        }
    }
}
