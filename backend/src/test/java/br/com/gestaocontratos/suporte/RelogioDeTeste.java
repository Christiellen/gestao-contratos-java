package br.com.gestaocontratos.suporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio que os testes podem parar e adiantar (o equivalente ao travelTo do Laravel). */
public class RelogioDeTeste extends Clock {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private volatile Instant agora = Instant.now();

    public void irPara(String dataHora) {
        this.agora = LocalDateTime.parse(dataHora).atZone(FUSO).toInstant();
    }

    public void avancar(Duration duracao) {
        this.agora = agora.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(agora, zone);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
