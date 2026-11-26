/*
 * Este archivo estructura cualquier listado largo que se muestra dividido en páginas
 * dentro de la tienda o los paneles internos, como el catálogo de productos, el historial
 * de pedidos o la lista de usuarios. Incluye los resultados visibles y los datos de navegación.
 */
package com.tecnicsecure.dto.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        // Lista de elementos que se muestran en la página actual
        List<T> contenido,
        // Número de la página que se está consultando (comienza en 0)
        int pagina,
        // Cantidad máxima de elementos que caben en cada página
        int tamanio,
        // Total de elementos encontrados sumando todas las páginas
        long totalElementos,
        // Cantidad total de páginas disponibles para recorrer
        int totalPaginas,
        // Indica si ya se llegó a la última página del listado
        boolean ultima
) {
    // Toma una página obtenida de los registros guardados y prepara cada elemento para mostrarlo en pantalla
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
