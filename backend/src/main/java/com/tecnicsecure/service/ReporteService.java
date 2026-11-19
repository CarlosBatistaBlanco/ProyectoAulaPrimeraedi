package com.tecnicsecure.service;

import com.tecnicsecure.dto.reporte.ProductoVendido;
import com.tecnicsecure.dto.reporte.ResumenReporte;
import com.tecnicsecure.entity.Pedido;

import java.time.LocalDate;
import java.util.List;

public interface ReporteService {

    ResumenReporte resumen(LocalDate desde, LocalDate hasta);

    List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite);

    List<Pedido> pedidosDelPeriodo(LocalDate desde, LocalDate hasta, String estado);
}
