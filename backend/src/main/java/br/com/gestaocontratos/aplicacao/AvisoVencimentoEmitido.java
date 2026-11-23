package br.com.gestaocontratos.aplicacao;

import java.time.LocalDate;
import java.util.List;

/**
 * Publicado quando um aviso de vencimento é registrado. O envio do e-mail acontece depois
 * do commit e fora da requisição (ver infraestrutura.EnvioAvisoVencimento).
 */
public record AvisoVencimentoEmitido(
        Long contratoId,
        String numero,
        String contratada,
        String objeto,
        LocalDate dataFim,
        int marco,
        int diasRestantes,
        List<Destinatario> destinatarios) {

    public record Destinatario(Long id, String nome, String email) {
    }
}
