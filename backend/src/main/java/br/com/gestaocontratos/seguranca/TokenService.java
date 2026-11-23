package br.com.gestaocontratos.seguranca;

import br.com.gestaocontratos.aplicacao.Relogio;
import br.com.gestaocontratos.dominio.modelo.TokenAcesso;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.TokenAcessoRepositorio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tokens de acesso opacos, como os do Laravel Sanctum: o texto aleatório vai para o cliente
 * e o banco guarda só o SHA-256. Diferente de um JWT, o token pode ser revogado no logout.
 */
@Service
public class TokenService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TokenAcessoRepositorio tokens;
    private final Relogio relogio;
    private final Duration validade;

    public TokenService(TokenAcessoRepositorio tokens, Relogio relogio,
                        @Value("${gestao-contratos.token.validade:8h}") Duration validade) {
        this.tokens = tokens;
        this.relogio = relogio;
        this.validade = validade;
    }

    @Transactional
    public String emitir(Usuario usuario) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new TokenAcesso(usuario, hash(token), relogio.agora(), relogio.agora().plus(validade)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<SessaoUsuario> autenticar(String token) {
        return tokens.buscarPorHash(hash(token))
                .filter(t -> !t.expiradoEm(relogio.agora()))
                .map(t -> new SessaoUsuario(t.getUsuario(), t.getId()));
    }

    @Transactional
    public void revogar(Long tokenId) {
        tokens.deleteById(tokenId);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
