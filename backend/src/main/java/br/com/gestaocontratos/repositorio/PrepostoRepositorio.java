package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.Preposto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrepostoRepositorio extends JpaRepository<Preposto, Long> {
}
