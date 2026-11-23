package br.com.gestaocontratos.dominio;

public enum TipoFiscal implements ComValor {
    TECNICO("tecnico", "Fiscal técnico"),
    REQUISITANTE("requisitante", "Fiscal requisitante"),
    ADMINISTRATIVO("administrativo", "Fiscal administrativo");

    private final String valor;
    private final String rotulo;

    TipoFiscal(String valor, String rotulo) {
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
