package br.com.gestaocontratos.dominio;

public enum TipoAditivo implements ComValor {
    PRAZO("prazo"),
    VALOR("valor"),
    PRAZO_E_VALOR("prazo_valor");

    private final String valor;

    TipoAditivo(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }

    public boolean alteraPrazo() {
        return this == PRAZO || this == PRAZO_E_VALOR;
    }

    public boolean alteraValor() {
        return this == VALOR || this == PRAZO_E_VALOR;
    }
}
