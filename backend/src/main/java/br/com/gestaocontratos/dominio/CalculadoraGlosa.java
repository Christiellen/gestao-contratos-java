package br.com.gestaocontratos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Cálculo de glosas pelo instrumento de medição de resultado (IMR).
 * Todo o cálculo usa BigDecimal: dinheiro nunca passa por ponto flutuante.
 */
public final class CalculadoraGlosa {

    /** Teto da soma das glosas sobre o valor da OS, em %. */
    public static final BigDecimal TETO_PERCENTUAL = new BigDecimal("30.00");

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    /** Resultado do indicador: 100 * realizado / previsto, com 2 casas. */
    public BigDecimal resultado(BigDecimal realizado, BigDecimal previsto) {
        if (previsto.signum() <= 0) {
            return CEM.setScale(2);
        }
        return realizado.multiply(CEM).divide(previsto, 2, RoundingMode.HALF_UP);
    }

    /** Glosa (%) da faixa em que o resultado se encaixa. */
    public BigDecimal glosaDaFaixa(List<Faixa> faixas, BigDecimal resultado) {
        List<Faixa> ordenadas = faixas.stream().sorted(Comparator.comparing(Faixa::minimo).reversed()).toList();
        for (Faixa faixa : ordenadas) {
            if (resultado.compareTo(faixa.minimo()) >= 0) {
                return faixa.glosa().setScale(2, RoundingMode.HALF_UP);
            }
        }
        // Abaixo de todas as faixas: aplica a glosa da faixa mais baixa.
        return ordenadas.isEmpty() ? BigDecimal.ZERO.setScale(2) : ordenadas.getLast().glosa().setScale(2, RoundingMode.HALF_UP);
    }

    /** Soma das glosas dos indicadores, limitada ao teto. */
    public BigDecimal percentualTotal(Collection<BigDecimal> percentuais) {
        BigDecimal soma = percentuais.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.min(TETO_PERCENTUAL).setScale(2, RoundingMode.HALF_UP);
    }

    /** Aplica o percentual sobre o valor estimado, arredondando a glosa ao centavo. */
    public ValoresGlosa aplicar(BigDecimal valorEstimado, BigDecimal percentual) {
        BigDecimal valor = valorEstimado.setScale(2, RoundingMode.HALF_UP);
        BigDecimal glosa = valor.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);
        return new ValoresGlosa(glosa, valor.subtract(glosa));
    }

    public record ValoresGlosa(BigDecimal valorGlosa, BigDecimal valorFinal) {
    }
}
