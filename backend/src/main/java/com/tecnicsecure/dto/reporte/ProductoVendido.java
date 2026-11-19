package com.tecnicsecure.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoVendido(
        Integer idProducto,
        String nombre,
        Long unidades,
        BigDecimal ingresos
) {
}
