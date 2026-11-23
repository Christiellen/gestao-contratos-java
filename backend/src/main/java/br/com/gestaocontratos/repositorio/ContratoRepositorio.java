package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.Contrato;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoRepositorio extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {

    @Override
    @EntityGraph(attributePaths = {"gestor", "fiscal"})
    Page<Contrato> findAll(Specification<Contrato> filtro, Pageable pagina);

    @Override
    @EntityGraph(attributePaths = {"gestor", "fiscal"})
    List<Contrato> findAll(Specification<Contrato> filtro, Sort ordem);

    /** Trava a linha do contrato até o fim da transação (numeração de OS sem colisão). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> travar(@Param("id") Long id);

    /** Inclui os excluídos logicamente: o número continua reservado, como na restrição única do banco. */
    @Query(value = "select exists(select 1 from contratos where numero = :numero and id <> :ignorarId)", nativeQuery = true)
    boolean numeroEmUso(@Param("numero") String numero, @Param("ignorarId") long ignorarId);

    /** Null quando não há contratos vigentes. */
    @Query("select sum(c.valorGlobal) from Contrato c where c.dataFim >= :hoje")
    BigDecimal somarValorVigente(@Param("hoje") LocalDate hoje);

    @EntityGraph(attributePaths = {"gestor", "fiscal"})
    @Query("select c from Contrato c where c.dataFim between :inicio and :fim order by c.dataFim, c.id")
    List<Contrato> vencendoEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
