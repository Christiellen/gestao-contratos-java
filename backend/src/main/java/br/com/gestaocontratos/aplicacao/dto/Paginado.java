package br.com.gestaocontratos.aplicacao.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página no formato que o front-end espera: {"data": [...], "meta": {"current_page": 1, ...}}. */
public record Paginado<T>(List<T> data, Meta meta) {

    public record Meta(int currentPage, int lastPage, int perPage, long total) {
    }

    public static <E, T> Paginado<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new Paginado<>(
                pagina.getContent().stream().map(conversor).toList(),
                new Meta(pagina.getNumber() + 1, Math.max(1, pagina.getTotalPages()), pagina.getSize(), pagina.getTotalElements()));
    }
}
