package br.com.gestaocontratos.dominio;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum cujo valor gravado no banco e trafegado na API é um texto (ex.: "em_preparacao"),
 * igual ao da versão em PHP. Mantém o contrato da API e o esquema do banco estáveis
 * mesmo que o nome da constante Java mude.
 */
public interface ComValor {

    String valor();

    static <E extends Enum<E> & ComValor> Optional<E> deValor(Class<E> tipo, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants()).filter(e -> e.valor().equals(valor)).findFirst();
    }

    static <E extends Enum<E> & ComValor> E deValorObrigatorio(Class<E> tipo, String valor) {
        return deValor(tipo, valor)
                .orElseThrow(() -> new IllegalArgumentException("Valor desconhecido para " + tipo.getSimpleName() + ": " + valor));
    }
}
