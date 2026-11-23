package br.com.gestaocontratos.aplicacao;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a).");
    }
}
