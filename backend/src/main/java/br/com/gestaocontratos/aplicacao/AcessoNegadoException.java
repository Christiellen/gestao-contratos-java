package br.com.gestaocontratos.aplicacao;

/** O usuário está autenticado, mas o perfil não permite a ação (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Você não tem permissão para esta ação.");
    }

    public static void exigir(boolean permitido) {
        if (!permitido) {
            throw new AcessoNegadoException();
        }
    }
}
