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

    @Override
    @Transactional(readOnly = true)
    public ResumenReporte resumen(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = inicioDe(desde);
        LocalDateTime fin = finDe(hasta);

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

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> pedidosDelPeriodo(LocalDate desde, LocalDate hasta, String estado) {
        return pedidoRepository.findEnRango(inicioDe(desde), finDe(hasta), estado);
    }

    private LocalDateTime inicioDe(LocalDate fecha) {
        return fecha == null ? LocalDate.now().withDayOfMonth(1).atStartOfDay() : fecha.atStartOfDay();
    }

    private LocalDateTime finDe(LocalDate fecha) {
        return fecha == null ? LocalDateTime.now() : fecha.atTime(LocalTime.MAX);
    }

    private BigDecimal toBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
    }
}
