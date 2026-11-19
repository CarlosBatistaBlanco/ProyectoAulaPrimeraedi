package com.tecnicsecure.dto.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Envoltorio de paginacion independiente de la version de Spring. */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {
    public static <E, T> PageResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
