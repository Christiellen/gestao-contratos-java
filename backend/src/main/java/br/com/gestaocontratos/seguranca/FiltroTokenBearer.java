package br.com.gestaocontratos.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lê "Authorization: Bearer <token>" e, se o token for válido, autentica a requisição. */
public class FiltroTokenBearer extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final TokenService tokens;

    public FiltroTokenBearer(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            tokens.autenticar(cabecalho.substring(PREFIXO.length()).trim()).ifPresent(sessao -> {
                var perfil = new SimpleGrantedAuthority("PERFIL_" + sessao.usuario().getPerfil().valor());
                var autenticacao = new UsernamePasswordAuthenticationToken(sessao, null, List.of(perfil));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            });
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
