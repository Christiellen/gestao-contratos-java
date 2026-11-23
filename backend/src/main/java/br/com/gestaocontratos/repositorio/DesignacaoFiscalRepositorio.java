package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignacaoFiscalRepositorio extends JpaRepository<DesignacaoFiscal, Long> {

    String EM_VIGOR = " d.dataInicio <= :data and (d.dataFim is null or d.dataFim >= :data) ";

    @Query("select d from DesignacaoFiscal d join fetch d.usuario where d.contrato.id = :contratoId and" + EM_VIGOR)
    List<DesignacaoFiscal> emVigor(@Param("contratoId") Long contratoId, @Param("data") LocalDate data);

    @Query("select count(d) from DesignacaoFiscal d where d.contrato.id = :contratoId and d.usuario.id = :usuarioId"
            + " and d.tipo in :tipos and" + EM_VIGOR)
    long contarFiscalEmVigor(@Param("contratoId") Long contratoId, @Param("usuarioId") Long usuarioId,
                          @Param("tipos") Collection<TipoFiscal> tipos, @Param("data") LocalDate data);
}
