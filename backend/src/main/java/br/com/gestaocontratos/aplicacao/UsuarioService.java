package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.aplicacao.dto.Visoes.UsuarioVisao;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepositorio usuarios;
    private final ConversorVisoes visoes;

    public UsuarioService(UsuarioRepositorio usuarios, ConversorVisoes visoes) {
        this.usuarios = usuarios;
        this.visoes = visoes;
    }

    /** Pessoas que podem ser gestoras ou fiscais (para os campos do formulário). */
    public List<UsuarioVisao> gestoresEFiscais() {
        return visoes.usuarios(usuarios.listarPorPerfis(List.of(Perfil.ADMINISTRADOR, Perfil.GESTOR)));
    }
}
