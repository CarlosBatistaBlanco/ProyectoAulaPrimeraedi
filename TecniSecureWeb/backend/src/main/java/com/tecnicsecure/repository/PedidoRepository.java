/*
 * Este archivo sirve como el buscador que consulta, guarda y resume las compras
 * registradas en la tienda. Se usa cuando el cliente abre Mis Pedidos, cuando un
 * empleado atiende las ventas del día o cuando el administrador consulta el Dashboard y Reportes.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    // Busca por páginas las compras hechas por un cliente específico para no cargar todo a la vez
    Page<Pedido> findByClienteIdCliente(Integer idCliente, Pageable pageable);

    // Borra todos los pedidos pertenecientes a un cliente concreto
    void deleteByClienteIdCliente(Integer idCliente);

    // Busca por páginas las compras que están en una etapa determinada (como pendiente o enviado)
    Page<Pedido> findByEstado(String estado, Pageable pageable);

    // Cuenta cuántos pedidos hay actualmente en una etapa concreta
    long countByEstado(String estado);

    // Suma el dinero total de todas las ventas válidas, dejando fuera los pedidos cancelados
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.estado <> 'cancelado'")
    BigDecimal sumarIngresosTotales();

    // Suma el dinero de las ventas válidas realizadas entre dos fechas, sin contar las canceladas
    @Query("""
            SELECT COALESCE(SUM(p.total), 0) FROM Pedido p
            WHERE p.estado <> 'cancelado' AND p.creadoEn BETWEEN :desde AND :hasta
            """)
    BigDecimal sumarIngresosEnRango(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    // Trae las compras hechas entre dos fechas, con la opción de filtrar además por una etapa específica
    @Query("""
            SELECT p FROM Pedido p
            WHERE p.creadoEn BETWEEN :desde AND :hasta
              AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> findEnRango(@Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             @Param("estado") String estado);

    // Trae las 10 compras más recientes de toda la tienda para el resumen rápido del panel
    List<Pedido> findTop10ByOrderByCreadoEnDesc();
}
