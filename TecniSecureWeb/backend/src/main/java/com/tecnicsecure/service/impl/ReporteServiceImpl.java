/*
 * Este archivo calcula las cifras y estadisticas principales del negocio para el administrador:
 * ingresos del periodo y acumulados, cantidad de pedidos reales, alertas de productos por
 * agotarse, solicitudes pendientes y los equipos mas vendidos. Su trabajo se ve en las tarjetas
 * de resumen y en las graficas o descargas de la seccion Reportes.
 */
package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.reporte.ProductoVendido;
import com.tecnicsecure.dto.reporte.ResumenReporte;
import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.ReporteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudServicioRepository solicitudRepository;
    private final ContactoRepository contactoRepository;

    // Prepara el acceso a ventas, productos, cuentas, solicitudes de servicio y mensajes de contacto.
    public ReporteServiceImpl(PedidoRepository pedidoRepository,
                              PedidoDetalleRepository pedidoDetalleRepository,
                              ProductoRepository productoRepository,
                              UsuarioRepository usuarioRepository,
                              SolicitudServicioRepository solicitudRepository,
                              ContactoRepository contactoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoDetalleRepository = pedidoDetalleRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.contactoRepository = contactoRepository;
    }

    // Reune en una sola vista las ventas del periodo, ingresos totales, alertas de bodega, casos pendientes y los 5 productos mas vendidos.
    @Override
    @Transactional(readOnly = true)
    public ResumenReporte resumen(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = inicioDe(desde);
        LocalDateTime fin = finDe(hasta);

        // Cuenta unicamente los pedidos validos del periodo dejando por fuera los que fueron cancelados.
        List<Pedido> pedidos = pedidoRepository.findEnRango(inicio, fin, null);
        long cantidad = pedidos.stream().filter(p -> !"cancelado".equals(p.getEstado())).count();

        return new ResumenReporte(
                inicio, fin,
                cantidad,
                pedidoRepository.sumarIngresosEnRango(inicio, fin),
                pedidoRepository.sumarIngresosTotales(),
                (long) productoRepository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count(),
                productoRepository.countProductosConStockBajo(),
                usuarioRepository.countByActivoTrue(),
                solicitudRepository.countByEstado("pendiente"),
                contactoRepository.countByLeidoFalse(),
                productosMasVendidos(desde, hasta, 5));
    }

    // Arma la lista de los productos con mas unidades vendidas y el dinero generado entre las fechas elegidas.
    @Override
    @Transactional(readOnly = true)
    public List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        return pedidoDetalleRepository.productosMasVendidos(inicioDe(desde), finDe(hasta)).stream()
                .limit(Math.max(1, limite))
                .map(fila -> new ProductoVendido(
                        (Integer) fila[0],
                        (String) fila[1],
                        ((Number) fila[2]).longValue(),
                        toBigDecimal(fila[3])))
                .toList();
    }

    // Trae el listado de pedidos realizados en un rango de fechas, con opcion de filtrar por estado para descargar reportes.
    @Override
    @Transactional(readOnly = true)
    public List<Pedido> pedidosDelPeriodo(LocalDate desde, LocalDate hasta, String estado) {
        return pedidoRepository.findEnRango(inicioDe(desde), finDe(hasta), estado);
    }

    // Si no se eligio fecha de inicio, toma automaticamente el primer dia del mes actual.
    private LocalDateTime inicioDe(LocalDate fecha) {
        return fecha == null ? LocalDate.now().withDayOfMonth(1).atStartOfDay() : fecha.atStartOfDay();
    }

    // Si no se eligio fecha final, toma la hora actual; si se eligio, abarca hasta el ultimo segundo de ese dia.
    private LocalDateTime finDe(LocalDate fecha) {
        return fecha == null ? LocalDateTime.now() : fecha.atTime(LocalTime.MAX);
    }

    // Convierte de forma segura cualquier cifra de ventas a un valor de dinero exacto sin perder centavos.
    private BigDecimal toBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
    }
}
