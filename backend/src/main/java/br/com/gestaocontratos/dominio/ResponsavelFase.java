package br.com.gestaocontratos.dominio;

public enum ResponsavelFase implements ComValor {
    GESTOR("gestor", "Gestor do contrato"),
    FISCAL_TECNICO("fiscal_tecnico", "Fiscal técnico"),
    FISCAL_ADMINISTRATIVO("fiscal_administrativo", "Fiscal administrativo");

    private final String valor;
    private final String rotulo;

    ResponsavelFase(String valor, String rotulo) {
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
