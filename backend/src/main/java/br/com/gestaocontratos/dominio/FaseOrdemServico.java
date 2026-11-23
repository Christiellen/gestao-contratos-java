package br.com.gestaocontratos.dominio;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fases da ordem de serviço, da emissão ao pagamento.
 *
 * Cada fase sabe qual é a próxima e quem pode avançá-la: o gestor do contrato,
 * o fiscal técnico ou o fiscal administrativo designados (o administrador pode tudo).
 * A ordem das constantes é a ordem do fluxo.
 */
public enum FaseOrdemServico implements ComValor {
    EM_PREPARACAO("em_preparacao", "Em preparação"),
    EM_EXECUCAO("em_execucao", "Em execução"),
    FISCALIZACAO_TECNICA("fiscalizacao_tecnica", "Fiscalização técnica"),
    FISCALIZACAO_ADMINISTRATIVA("fiscalizacao_administrativa", "Fiscalização administrativa"),
    RECEBIMENTO_DEFINITIVO("recebimento_definitivo", "Recebimento definitivo"),
    NOTA_FISCAL("nota_fiscal", "Nota fiscal"),
    ANALISE_PAGAMENTO("analise_pagamento", "Análise de pagamento"),
    PAGAMENTO_AUTORIZADO("pagamento_autorizado", "Pagamento autorizado"),
    ENCERRADA("encerrada", "Encerrada"),
    CANCELADA("cancelada", "Cancelada");

    /** Fases em que a OS ainda pode ser cancelada: depois da nota fiscal, não pode mais. */
    private static final Set<FaseOrdemServico> CANCELAVEIS =
            EnumSet.of(EM_PREPARACAO, EM_EXECUCAO, FISCALIZACAO_TECNICA, FISCALIZACAO_ADMINISTRATIVA, RECEBIMENTO_DEFINITIVO);

    private final String valor;
    private final String rotulo;

    FaseOrdemServico(String valor, String rotulo) {
        this.valor = valor;
        this.rotulo = rotulo;
    }

    @Override
    public String valor() {
        return valor;
    }

    public String rotulo() {
        return rotulo;
    }

    public Optional<FaseOrdemServico> proxima() {
        if (finalizada()) {
            return Optional.empty();
        }
        return Optional.of(values()[ordinal() + 1]);
    }

    /** Quem pode avançar a OS a partir desta fase (vazio se já finalizada). */
    public Optional<ResponsavelFase> responsavel() {
        return switch (this) {
            case FISCALIZACAO_TECNICA -> Optional.of(ResponsavelFase.FISCAL_TECNICO);
            case FISCALIZACAO_ADMINISTRATIVA -> Optional.of(ResponsavelFase.FISCAL_ADMINISTRATIVO);
            case ENCERRADA, CANCELADA -> Optional.empty();
            default -> Optional.of(ResponsavelFase.GESTOR);
        };
    }

    /** O fiscal técnico pode devolver a entrega para correção. */
    public boolean podeDevolver() {
        return this == FISCALIZACAO_TECNICA;
    }

    public boolean podeCancelar() {
        return CANCELAVEIS.contains(this);
    }

    /** Fases cujo valor já consome o saldo do empenho. */
    public boolean consomeEmpenho() {
        return this == PAGAMENTO_AUTORIZADO || this == ENCERRADA;
    }

    public boolean finalizada() {
        return this == ENCERRADA || this == CANCELADA;
    }

    public static List<FaseOrdemServico> queConsomemEmpenho() {
        return Arrays.stream(values()).filter(FaseOrdemServico::consomeEmpenho).toList();
    }

    /** Fases do fluxo normal, sem o cancelamento. */
    public static List<FaseOrdemServico> fluxo() {
        return Arrays.stream(values()).filter(f -> f != CANCELADA).toList();
    }
}
