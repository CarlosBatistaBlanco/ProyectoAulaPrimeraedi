/*
 * Este archivo sirve como el buscador que consulta los artículos incluidos dentro de
 * cada compra realizada en la tienda. Se utiliza para calcular los reportes de productos
 * más vendidos e ingresos generados en la sección de Reportes y Dashboard del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Integer> {

    // Calcula el listado de productos más vendidos (unidades e ingresos) sin contar compras canceladas, con filtro opcional de fechas
    @Query("""
            SELECT d.producto.idProducto, d.producto.nombre, SUM(d.cantidad), SUM(d.cantidad * d.precioUnitario)
            FROM PedidoDetalle d
            WHERE d.pedido.estado <> 'cancelado'
              AND (:desde IS NULL OR d.pedido.creadoEn >= :desde)
              AND (:hasta IS NULL OR d.pedido.creadoEn <= :hasta)
            GROUP BY d.producto.idProducto, d.producto.nombre
            ORDER BY SUM(d.cantidad) DESC
            """)
    List<Object[]> productosMasVendidos(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);
}
