package com.tecnicsecure.repository;

import com.tecnicsecure.entity.InventarioMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventarioMovimientoRepository extends JpaRepository<InventarioMovimiento, Integer> {

    List<InventarioMovimiento> findByIdProductoOrderByCreadoEnDesc(Integer idProducto);

    List<InventarioMovimiento> findTop50ByOrderByCreadoEnDesc();
}
