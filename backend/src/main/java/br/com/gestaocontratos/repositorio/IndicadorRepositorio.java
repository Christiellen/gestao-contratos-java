package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.Indicador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndicadorRepositorio extends JpaRepository<Indicador, Long> {

    List<Indicador> findByContratoIdOrderBySigla(Long contratoId);

    boolean existsByContratoIdAndSiglaIgnoreCase(Long contratoId, String sigla);
}
