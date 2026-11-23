package br.com.gestaocontratos.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalInt;

/**
 * Regras de vencimento de contratos.
 *
 * Os avisos são enviados em três marcos (90, 60 e 30 dias antes do fim da vigência).
 * Em cada execução diária vale o menor marco já alcançado, para que um contrato
 * cadastrado com 45 dias restantes receba o aviso de 60 dias, e não o de 90.
 */
public final class RegrasVencimento {

    /** Marcos de aviso, em dias antes do vencimento, do maior para o menor. */
    public static final List<Integer> MARCOS = List.of(90, 60, 30);

    /** A partir de quantos dias restantes o contrato passa a "A vencer". */
    public static final int JANELA_A_VENCER = 90;

    public int diasParaVencer(LocalDate dataFim, LocalDate hoje) {
        return (int) ChronoUnit.DAYS.between(hoje, dataFim);
    }

    public SituacaoContrato situacao(LocalDate dataFim, LocalDate hoje) {
        int dias = diasParaVencer(dataFim, hoje);
        if (dias < 0) {
            return SituacaoContrato.VENCIDO;
        }
        return dias <= JANELA_A_VENCER ? SituacaoContrato.A_VENCER : SituacaoContrato.VIGENTE;
    }

    /** Marco de aviso que se aplica hoje, ou vazio fora da janela (mais de 90 dias ou já vencido). */
    public OptionalInt marcoAplicavel(LocalDate dataFim, LocalDate hoje) {
        int dias = diasParaVencer(dataFim, hoje);
        if (dias < 0) {
            return OptionalInt.empty();
        }
        return MARCOS.stream().mapToInt(Integer::intValue).filter(marco -> dias <= marco).min();
    }
}
