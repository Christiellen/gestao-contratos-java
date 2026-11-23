package br.com.gestaocontratos.dominio;

public enum Perfil implements ComValor {
    ADMINISTRADOR("administrador", "Administrador"),
    GESTOR("gestor", "Gestor de contratos"),
    CONSULTA("consulta", "Consulta");

    private final String valor;
    private final String rotulo;

    Perfil(String valor, String rotulo) {
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
