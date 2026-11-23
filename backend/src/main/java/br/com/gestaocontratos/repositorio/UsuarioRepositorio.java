package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Query("select u from Usuario u where u.perfil in :perfis order by u.nome")
    List<Usuario> listarPorPerfis(@Param("perfis") Collection<Perfil> perfis);
}
