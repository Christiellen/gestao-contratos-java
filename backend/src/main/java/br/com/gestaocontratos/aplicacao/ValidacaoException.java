package br.com.gestaocontratos.aplicacao;

import java.util.List;
import java.util.Map;

public class ValidacaoException extends RuntimeException {

    private final transient Map<String, List<String>> erros;

    public ValidacaoException(Map<String, List<String>> erros) {
        super(resumo(erros));
        this.erros = Map.copyOf(erros);
    }

    public static ValidacaoException doCampo(String campo, String mensagem) {
        return new ValidacaoException(Map.of(campo, List.of(mensagem)));
    }

    public Map<String, List<String>> getErros() {
        return erros;
    }

    /** "Primeira mensagem (e mais 2 erros)", como o Laravel faz em inglês. */
    private static String resumo(Map<String, List<String>> erros) {
        int total = erros.values().stream().mapToInt(List::size).sum();
        String primeira = erros.values().iterator().next().getFirst();
        if (total == 1) {
            return primeira;
        }
        return primeira + " (e mais " + (total - 1) + (total == 2 ? " erro)" : " erros)");
    }
}
