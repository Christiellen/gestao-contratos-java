package br.com.gestaocontratos.seguranca;

import br.com.gestaocontratos.aplicacao.Relogio;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limita as tentativas de login por e-mail + IP (janela deslizante de 1 minuto) para
 * dificultar força bruta. Em memória: com várias instâncias da API, trocar por Redis.
 */
@Component
public class LimitadorLogin {

    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();
    private final Relogio relogio;
    private final int limite;

    public LimitadorLogin(Relogio relogio, @Value("${gestao-contratos.login.tentativas-por-minuto:5}") int limite) {
        this.relogio = relogio;
        this.limite = limite;
    }

    /** Registra a tentativa; lança MuitasTentativasException se o limite já foi atingido. */
    public void registrar(String email, String ip) {
        String chave = (email == null ? "" : email.toLowerCase(Locale.ROOT)) + "|" + ip;
        Instant agora = relogio.agora();
        Deque<Instant> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && !fila.peekFirst().isAfter(agora.minus(JANELA))) {
                fila.pollFirst();
            }
            if (fila.size() >= limite) {
                throw new MuitasTentativasException();
            }
            fila.addLast(agora);
        }
    }

    /** Usado nos testes. */
    public void limpar() {
        tentativas.clear();
    }
}
