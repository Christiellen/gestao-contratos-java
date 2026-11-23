package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepositorio extends JpaRepository<OrdemServico, Long>, JpaSpecificationExecutor<OrdemServico> {

    @Override
    @EntityGraph(attributePaths = {"contrato"})
    Page<OrdemServico> findAll(Specification<OrdemServico> filtro, Pageable pagina);

    long countByContratoIdAndNumeroEndingWith(Long contratoId, String sufixo);

    /** Valor já comprometido no empenho por OS com pagamento autorizado ou encerradas (null se nenhuma). */
    @Query("select sum(o.valorFinal) from OrdemServico o"
            + " where o.empenho.id = :empenhoId and o.fase in :fases and o.id <> :ignorarId")
    BigDecimal valorUtilizado(@Param("empenhoId") Long empenhoId, @Param("fases") Collection<FaseOrdemServico> fases,
                              @Param("ignorarId") long ignorarId);

    List<OrdemServico> findByFase(FaseOrdemServico fase);
}
