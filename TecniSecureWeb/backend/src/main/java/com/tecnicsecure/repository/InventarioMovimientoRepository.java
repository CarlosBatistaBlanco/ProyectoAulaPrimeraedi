/*
 * Este archivo sirve como el buscador que guarda y consulta el registro de entradas y
 * salidas de bodega de todos los productos. Se utiliza en la sección de Inventario del
 * panel para revisar el historial de un artículo o los últimos movimientos de la tienda.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.InventarioMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventarioMovimientoRepository extends JpaRepository<InventarioMovimiento, Integer> {

    // Trae todas las entradas y salidas de bodega de un producto, de la más reciente a la más antigua
    List<InventarioMovimiento> findByIdProductoOrderByCreadoEnDesc(Integer idProducto);

    // Trae los 50 movimientos de bodega más recientes registrados en toda la tienda
    List<InventarioMovimiento> findTop50ByOrderByCreadoEnDesc();
}
