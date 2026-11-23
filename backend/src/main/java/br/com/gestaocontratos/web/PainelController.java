package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.ContratoService;
import br.com.gestaocontratos.aplicacao.UsuarioService;
import br.com.gestaocontratos.aplicacao.dto.Dados;
import br.com.gestaocontratos.aplicacao.dto.Visoes.PainelVisao;
import br.com.gestaocontratos.aplicacao.dto.Visoes.UsuarioVisao;
import br.com.gestaocontratos.seguranca.SessaoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Painel")
public class PainelController {

    private final ContratoService contratos;
    private final UsuarioService usuarios;

    public PainelController(ContratoService contratos, UsuarioService usuarios) {
        this.contratos = contratos;
        this.usuarios = usuarios;
    }

    @GetMapping("/painel")
    @Operation(summary = "Totais por situação, valor vigente e próximos vencimentos")
    public PainelVisao painel(@AuthenticationPrincipal SessaoUsuario sessao) {
        return contratos.painel(sessao.usuario());
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Pessoas que podem ser gestoras ou fiscais")
    public Dados<List<UsuarioVisao>> usuarios() {
        return new Dados<>(usuarios.gestoresEFiscais());
    }
}
