package br.com.gestaocontratos.aplicacao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Data e hora "de agora" vindas de um Clock injetado. Nos testes o Clock é trocado
 * por um relógio controlável, o que permite simular a passagem dos dias.
 */
@Component
public class Relogio {

    private final Clock clock;

    public Relogio(Clock clock) {
        this.clock = clock;
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    public Instant agora() {
        return clock.instant();
    }

    public ZoneId fuso() {
        return clock.getZone();
    }
}
