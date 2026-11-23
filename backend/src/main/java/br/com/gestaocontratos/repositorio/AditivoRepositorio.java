package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.modelo.Aditivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AditivoRepositorio extends JpaRepository<Aditivo, Long> {

    @Query("select coalesce(max(a.numero), 0) from Aditivo a where a.contrato.id = :contratoId")
    int ultimoNumero(@Param("contratoId") Long contratoId);

    long countByContratoId(Long contratoId);
}
