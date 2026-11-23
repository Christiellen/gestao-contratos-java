package br.com.gestaocontratos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegrasVencimentoTest {

    private final RegrasVencimento regras = new RegrasVencimento();
    private final LocalDate hoje = LocalDate.of(2026, 3, 10);

    @Test
    void contaDiasAteOFim() {
        assertThat(regras.diasParaVencer(hoje.plusDays(30), hoje)).isEqualTo(30);
        assertThat(regras.diasParaVencer(hoje, hoje)).isZero();
        assertThat(regras.diasParaVencer(hoje.minusDays(5), hoje)).isEqualTo(-5);
    }

    @ParameterizedTest(name = "{0} dias -> {1}")
    @CsvSource({"-1, VENCIDO", "0, A_VENCER", "90, A_VENCER", "91, VIGENTE"})
    void classificaASituacao(int dias, SituacaoContrato esperada) {
        assertThat(regras.situacao(hoje.plusDays(dias), hoje)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "{0} dias -> marco {1}")
    @CsvSource({"90, 90", "75, 90", "60, 60", "45, 60", "30, 30", "1, 30", "0, 30"})
    void usaOMenorMarcoJaAlcancado(int dias, int marco) {
        assertThat(regras.marcoAplicavel(hoje.plusDays(dias), hoje)).isEqualTo(OptionalInt.of(marco));
    }

    @Test
    void foraDaJanelaNaoHaMarco() {
        assertThat(regras.marcoAplicavel(hoje.plusDays(91), hoje)).isEmpty();
        assertThat(regras.marcoAplicavel(hoje.minusDays(1), hoje)).isEmpty();
    }
}
