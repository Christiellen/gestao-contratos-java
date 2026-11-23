package br.com.gestaocontratos.dominio;

public enum Papel implements ComValor {
    TITULAR("titular"),
    SUBSTITUTO("substituto");

    private final String valor;

    Papel(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
