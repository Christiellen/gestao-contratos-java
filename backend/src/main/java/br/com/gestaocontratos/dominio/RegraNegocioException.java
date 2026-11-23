package br.com.gestaocontratos.dominio;

/**
 * Violação de uma regra do processo (ex.: avançar OS sem medições).
 * A API responde 422 com a mensagem, para o front-end exibir ao usuário.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
