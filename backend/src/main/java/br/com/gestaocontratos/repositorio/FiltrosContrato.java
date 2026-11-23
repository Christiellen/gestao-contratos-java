package br.com.gestaocontratos.repositorio;

import br.com.gestaocontratos.dominio.RegrasVencimento;
import br.com.gestaocontratos.dominio.SituacaoContrato;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros da listagem de contratos. A situação é calculada pela data de fim da vigência. */
public final class FiltrosContrato {

    private FiltrosContrato() {
    }

    public static Specification<Contrato> situacao(SituacaoContrato situacao, LocalDate hoje) {
        LocalDate limite = hoje.plusDays(RegrasVencimento.JANELA_A_VENCER);
        return (raiz, consulta, cb) -> switch (situacao) {
            case VENCIDO -> cb.lessThan(raiz.get("dataFim"), hoje);
            case A_VENCER -> cb.between(raiz.get("dataFim"), hoje, limite);
            case VIGENTE -> cb.greaterThan(raiz.get("dataFim"), limite);
        };
    }

    public static Specification<Contrato> busca(String termo) {
        String padrao = "%" + termo.trim().toLowerCase(Locale.ROOT) + "%";
        return (raiz, consulta, cb) -> cb.or(
                cb.like(cb.lower(raiz.get("numero")), padrao),
                cb.like(cb.lower(raiz.get("objeto")), padrao),
                cb.like(cb.lower(raiz.get("contratada")), padrao));
    }

    public static Specification<Contrato> gestor(Long gestorId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("gestor").get("id"), gestorId);
    }
}
