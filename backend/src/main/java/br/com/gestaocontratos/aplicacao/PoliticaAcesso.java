package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Administrador: tudo. Gestor: cadastra contratos e altera os que gere.
 * Consulta: apenas leitura.
 */
@Component
public class PoliticaAcesso {

    public boolean podeCriarContrato(Usuario usuario) {
        return usuario.isAdministrador() || usuario.isGestor();
    }

    public boolean podeAlterar(Usuario usuario, Contrato contrato) {
        return usuario.isAdministrador() || usuario.isGestor() && contrato.geridoPor(usuario);
    }

    public boolean podeExcluir(Usuario usuario) {
        return usuario.isAdministrador();
    }
}
