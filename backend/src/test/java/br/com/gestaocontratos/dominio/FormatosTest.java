package br.com.gestaocontratos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FormatosTest {

    @Test
    void formataMoedaNoPadraoBrasileiro() {
        assertThat(Formatos.moeda(new BigDecimal("50000"))).isEqualTo("50.000,00");
        assertThat(Formatos.moeda(new BigDecimal("1234567.8"))).isEqualTo("1.234.567,80");
    }

    @Test
    void formataDuracao() {
        assertThat(Formatos.duracao(3 * 86_400)).isEqualTo("3d 0h 0min");
        assertThat(Formatos.duracao(90_061)).isEqualTo("1d 1h 1min");
    }
}
