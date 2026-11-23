package br.com.gestaocontratos.seguranca;

import br.com.gestaocontratos.dominio.modelo.Usuario;

/** Quem está chamando a API e com qual token (para o logout revogar exatamente esse token). */
public record SessaoUsuario(Usuario usuario, Long tokenId) {
}
