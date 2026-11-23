package br.com.gestaocontratos.aplicacao;

import br.com.gestaocontratos.dominio.ComValor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Acumula erros de validação por campo e lança tudo de uma vez (HTTP 422), no mesmo formato
 * da API em PHP: {"message": "...", "errors": {"campo": ["mensagem"]}}.
 *
 * Cada regra ignora valores nulos (exceto "obrigatorio") e campos que já têm erro,
 * para mostrar ao usuário a primeira coisa a corrigir em cada campo.
 */
public final class Validacao {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<String, List<String>> erros = new LinkedHashMap<>();

    public Validacao erro(String campo, String mensagem) {
        erros.computeIfAbsent(campo, c -> new ArrayList<>()).add(mensagem);
        return this;
    }

    public boolean semErro(String campo) {
        return !erros.containsKey(campo);
    }

    /** Devolve true quando o valor foi informado. */
    public boolean obrigatorio(String campo, String rotulo, Object valor) {
        boolean vazio = valor == null
                || valor instanceof String texto && texto.isBlank()
                || valor instanceof Collection<?> lista && lista.isEmpty();
        if (vazio) {
            erro(campo, "O campo " + rotulo + " é obrigatório.");
        }
        return !vazio;
    }

    public void maximo(String campo, String rotulo, String valor, int max) {
        if (valor != null && semErro(campo) && valor.length() > max) {
            erro(campo, "O campo " + rotulo + " deve ter no máximo " + max + " caracteres.");
        }
    }

    public void minimo(String campo, String rotulo, BigDecimal valor, String min) {
        if (valor != null && semErro(campo) && valor.compareTo(new BigDecimal(min)) < 0) {
            erro(campo, "O campo " + rotulo + " deve ser no mínimo " + min + ".");
        }
    }

    public void entre(String campo, String rotulo, BigDecimal valor, String min, String max) {
        if (valor != null && semErro(campo)
                && (valor.compareTo(new BigDecimal(min)) < 0 || valor.compareTo(new BigDecimal(max)) > 0)) {
            erro(campo, "O campo " + rotulo + " deve estar entre " + min + " e " + max + ".");
        }
    }

    public void email(String campo, String valor) {
        if (valor != null && semErro(campo) && !EMAIL.matcher(valor).matches()) {
            erro(campo, "Informe um e-mail válido.");
        }
    }

    /** Converte o texto no enum correspondente; devolve null (e registra o erro) se não existir. */
    public <E extends Enum<E> & ComValor> E opcao(String campo, String rotulo, String valor, Class<E> tipo) {
        if (valor == null) {
            return null;
        }
        E encontrado = ComValor.deValor(tipo, valor).orElse(null);
        if (encontrado == null && semErro(campo)) {
            erro(campo, "O valor informado para " + rotulo + " não é válido.");
        }
        return encontrado;
    }

    /** Regra própria: registra a mensagem quando a condição de erro é verdadeira. */
    public void se(boolean condicaoDeErro, String campo, String mensagem) {
        if (condicaoDeErro && semErro(campo)) {
            erro(campo, mensagem);
        }
    }

    public void lancarSeHouverErros() {
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }
}
