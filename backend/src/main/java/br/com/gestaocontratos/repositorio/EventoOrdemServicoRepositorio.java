package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.EventoOrdemServico;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoOrdemServicoRepositorio extends JpaRepository<EventoOrdemServico, Long> {

    @Query("select e from EventoOrdemServico e where e.ocorridoEm between :inicio and :fim"
            + " order by e.ordemServico.id, e.ocorridoEm, e.id")
    List<EventoOrdemServico> doPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
