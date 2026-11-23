package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.AlertaVencimento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaVencimentoRepositorio extends JpaRepository<AlertaVencimento, Long> {

    boolean existsByContratoIdAndMarcoAndDataFimReferencia(Long contratoId, int marco, LocalDate dataFimReferencia);

    List<AlertaVencimento> findByContratoIdOrderByIdAsc(Long contratoId);
}
