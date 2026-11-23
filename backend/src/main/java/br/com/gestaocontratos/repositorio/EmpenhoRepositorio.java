package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.Empenho;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpenhoRepositorio extends JpaRepository<Empenho, Long> {

    boolean existsByContratoIdAndNumero(Long contratoId, String numero);

    boolean existsByIdAndContratoId(Long id, Long contratoId);
}
