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

    Page<Pedido> findByClienteIdCliente(Integer idCliente, Pageable pageable);

    void deleteByClienteIdCliente(Integer idCliente);

    Page<Pedido> findByEstado(String estado, Pageable pageable);

    long countByEstado(String estado);

    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.estado <> 'cancelado'")
    BigDecimal sumarIngresosTotales();

    @Query("""
            SELECT COALESCE(SUM(p.total), 0) FROM Pedido p
            WHERE p.estado <> 'cancelado' AND p.creadoEn BETWEEN :desde AND :hasta
            """)
    BigDecimal sumarIngresosEnRango(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT p FROM Pedido p
            WHERE p.creadoEn BETWEEN :desde AND :hasta
              AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> findEnRango(@Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             @Param("estado") String estado);

    List<Pedido> findTop10ByOrderByCreadoEnDesc();
}
