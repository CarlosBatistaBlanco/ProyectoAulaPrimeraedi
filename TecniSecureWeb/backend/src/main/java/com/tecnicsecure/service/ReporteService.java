/*
 * Este archivo define las consultas de cifras y estadisticas del negocio: ingresos por fechas,
 * productos que mas se venden y listados de pedidos para analisis o exportacion. Su trabajo se
 * ve en el resumen inicial y en la seccion de reportes del panel administrativo.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.reporte.ProductoVendido;
import com.tecnicsecure.dto.reporte.ResumenReporte;
import com.tecnicsecure.entity.Pedido;

import java.time.LocalDate;
import java.util.List;

public interface ReporteService {

    // Calcula el resumen general del negocio (ventas, pedidos, alertas de bodega y pendientes) entre dos fechas.
    ResumenReporte resumen(LocalDate desde, LocalDate hasta);

    // Arma el listado de los productos que mas unidades vendieron dentro del periodo elegido.
    List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite);

    // Trae todos los pedidos realizados en un rango de fechas, con opcion de filtrar por estado.
    List<Pedido> pedidosDelPeriodo(LocalDate desde, LocalDate hasta, String estado);
}
