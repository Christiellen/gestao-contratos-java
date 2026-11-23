package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.TokenAcesso;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenAcessoRepositorio extends JpaRepository<TokenAcesso, Long> {

    @Query("select t from TokenAcesso t join fetch t.usuario where t.hash = :hash")
    Optional<TokenAcesso> buscarPorHash(@Param("hash") String hash);
}
