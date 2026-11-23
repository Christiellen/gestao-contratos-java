package br.com.gestaocontratos.dominio;

public enum SituacaoContrato implements ComValor {
    VIGENTE("vigente", "Vigente"),
    A_VENCER("a_vencer", "A vencer"),
    VENCIDO("vencido", "Vencido");

    private final String valor;
    private final String rotulo;

    SituacaoContrato(String valor, String rotulo) {
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
}
