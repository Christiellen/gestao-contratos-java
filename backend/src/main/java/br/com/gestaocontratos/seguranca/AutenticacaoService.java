package br.com.gestaocontratos.seguranca;

import br.com.gestaocontratos.aplicacao.ConversorVisoes;
import br.com.gestaocontratos.aplicacao.Validacao;
import br.com.gestaocontratos.aplicacao.ValidacaoException;
import br.com.gestaocontratos.aplicacao.dto.Formularios.LoginForm;
import br.com.gestaocontratos.aplicacao.dto.Visoes.LoginVisao;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    private final LimitadorLogin limitador;
    private final ConversorVisoes visoes;

    public AutenticacaoService(UsuarioRepositorio usuarios, PasswordEncoder senhas, TokenService tokens,
                               LimitadorLogin limitador, ConversorVisoes visoes) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.limitador = limitador;
        this.visoes = visoes;
    }

    public LoginVisao entrar(LoginForm form, String ip) {
        limitador.registrar(form.email(), ip);

        Validacao v = new Validacao();
        if (v.obrigatorio("email", "e-mail", form.email())) {
            v.email("email", form.email());
        }
        v.obrigatorio("password", "senha", form.password());
        v.lancarSeHouverErros();

        Usuario usuario = usuarios.findByEmailIgnoreCase(form.email().trim())
                .filter(u -> senhas.matches(form.password(), u.getSenhaHash()))
                // Mesma mensagem para e-mail inexistente e senha errada: não revela quem tem conta.
                .orElseThrow(() -> ValidacaoException.doCampo("email", "E-mail ou senha inválidos."));
        return new LoginVisao(tokens.emitir(usuario), visoes.usuario(usuario));
    }
}
