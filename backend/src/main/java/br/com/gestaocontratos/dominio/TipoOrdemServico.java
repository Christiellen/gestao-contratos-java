package br.com.gestaocontratos.dominio;

public enum TipoOrdemServico implements ComValor {
    PROJETO("projeto", "Projeto"),
    SUSTENTACAO("sustentacao", "Sustentação"),
    DADOS("dados", "Dados");

    private final String valor;
    private final String rotulo;

    TipoOrdemServico(String valor, String rotulo) {
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
