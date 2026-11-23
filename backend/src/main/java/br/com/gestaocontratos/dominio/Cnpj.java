package br.com.gestaocontratos.dominio;

/** Valida o CNPJ pelos dígitos verificadores (aceita com ou sem máscara). */
public final class Cnpj {

    private Cnpj() {
    }

    public static String somenteDigitos(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("\\D", "");
    }

    public static boolean valido(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14 || d.chars().distinct().count() == 1) {
            return false;
        }
        for (int posicao : new int[] {12, 13}) {
            int soma = 0;
            int peso = posicao - 7;
            for (int i = 0; i < posicao; i++) {
                soma += (d.charAt(i) - '0') * peso--;
                if (peso < 2) {
                    peso = 9;
                }
            }
            int digito = soma % 11 < 2 ? 0 : 11 - soma % 11;
            if (d.charAt(posicao) - '0' != digito) {
                return false;
            }
        }
        return true;
    }
}
