package com.tecnicsecure.dto.reporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agregados usados por el dashboard y los reportes. */
public record ResumenReporte(
        LocalDateTime desde,
        LocalDateTime hasta,
        Long cantidadPedidos,
        java.math.BigDecimal ingresosPeriodo,
        java.math.BigDecimal ingresosHistoricos,
        Long productosActivos,
        Long productosStockBajo,
        Long usuariosActivos,
        Long solicitudesPendientes,
        Long mensajesContactoSinLeer,
        List<ProductoVendido> masVendidos
) {
}
