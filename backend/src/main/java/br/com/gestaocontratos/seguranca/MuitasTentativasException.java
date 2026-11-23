package br.com.gestaocontratos.seguranca;

public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas de login. Aguarde um minuto e tente de novo.");
    }
}
