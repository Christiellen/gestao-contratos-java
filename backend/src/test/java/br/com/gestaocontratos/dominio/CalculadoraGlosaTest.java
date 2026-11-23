package br.com.gestaocontratos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculadoraGlosaTest {

    private final CalculadoraGlosa calculadora = new CalculadoraGlosa();

    private final List<Faixa> faixas = List.of(faixa("95", "0"), faixa("90", "2"), faixa("80", "5"), faixa("0", "10"));

    private static Faixa faixa(String minimo, String glosa) {
        return new Faixa(new BigDecimal(minimo), new BigDecimal(glosa));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void resultadoEPercentualDoPrevisto() {
        assertThat(calculadora.resultado(bd("37"), bd("40"))).isEqualByComparingTo("92.5");
        assertThat(calculadora.resultado(bd("10"), bd("0"))).isEqualByComparingTo("100");
    }

    @ParameterizedTest(name = "resultado {0} -> glosa {1}%")
    @CsvSource({
        "100, 0",     // acima da meta
        "95, 0",      // exatamente na meta
        "94.99, 2",   // logo abaixo da meta
        "85, 5",      // faixa intermediária
        "40, 10",     // muito abaixo
    })
    void aplicaAGlosaDaFaixa(String resultado, String glosa) {
        assertThat(calculadora.glosaDaFaixa(faixas, bd(resultado))).isEqualByComparingTo(glosa);
    }

    @Test
    void faixasForaDeOrdemSaoAvaliadasDoMaiorMinimoParaOMenor() {
        List<Faixa> desordenadas = List.of(faixa("0", "5"), faixa("95", "0"));
        assertThat(calculadora.glosaDaFaixa(desordenadas, bd("96"))).isEqualByComparingTo("0");
        assertThat(calculadora.glosaDaFaixa(desordenadas, bd("50"))).isEqualByComparingTo("5");
    }

    @Test
    void somaDasGlosasRespeitaOTeto() {
        assertThat(calculadora.percentualTotal(List.of(bd("2"), bd("5")))).isEqualByComparingTo("7");
        assertThat(calculadora.percentualTotal(List.of(bd("10"), bd("10"), bd("10"), bd("10"))))
                .isEqualByComparingTo(CalculadoraGlosa.TETO_PERCENTUAL);
    }

    @Test
    void aplicaOPercentualArredondandoAoCentavo() {
        CalculadoraGlosa.ValoresGlosa valores = calculadora.aplicar(bd("124438.56"), bd("1.984"));
        assertThat(valores.valorGlosa()).isEqualTo(bd("2468.86"));
        assertThat(valores.valorFinal()).isEqualTo(bd("121969.70"));
    }
}
