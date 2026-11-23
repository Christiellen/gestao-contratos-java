package br.com.gestaocontratos.dominio;

import java.math.BigDecimal;

/**
 * Faixa de glosa de um indicador: se o resultado for maior ou igual ao mínimo,
 * aplica o percentual de glosa. Ex.: {minimo: 90, glosa: 2}.
 */
public record Faixa(BigDecimal minimo, BigDecimal glosa) {
}
