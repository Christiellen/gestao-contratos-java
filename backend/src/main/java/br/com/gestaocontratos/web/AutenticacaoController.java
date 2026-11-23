package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.ConversorVisoes;
import br.com.gestaocontratos.aplicacao.dto.Dados;
import br.com.gestaocontratos.aplicacao.dto.Formularios.LoginForm;
import br.com.gestaocontratos.aplicacao.dto.Visoes.LoginVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.UsuarioVisao;
import br.com.gestaocontratos.seguranca.AutenticacaoService;
import br.com.gestaocontratos.seguranca.SessaoUsuario;
import br.com.gestaocontratos.seguranca.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Autenticação")
public class AutenticacaoController {

    private final AutenticacaoService autenticacao;
    private final TokenService tokens;
    private final ConversorVisoes visoes;

    public AutenticacaoController(AutenticacaoService autenticacao, TokenService tokens, ConversorVisoes visoes) {
        this.autenticacao = autenticacao;
        this.tokens = tokens;
        this.visoes = visoes;
    }

    @PostMapping("/login")
    @Operation(summary = "Entra com e-mail e senha e recebe um token Bearer (válido por 8 horas)")
    public LoginVisao login(@RequestBody LoginForm form, HttpServletRequest requisicao) {
        return autenticacao.entrar(form, requisicao.getRemoteAddr());
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário dono do token")
    public Dados<UsuarioVisao> eu(@AuthenticationPrincipal SessaoUsuario sessao) {
        return new Dados<>(visoes.usuario(sessao.usuario()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o token usado na requisição")
    public ResponseEntity<Void> sair(@AuthenticationPrincipal SessaoUsuario sessao) {
        tokens.revogar(sessao.tokenId());
        return ResponseEntity.noContent().build();
    }
}
