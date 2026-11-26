/*
 * Este archivo sirve como el buscador que guarda y consulta la línea de tiempo con
 * todos los cambios de etapa que ha tenido un pedido. Se utiliza en el seguimiento de
 * Mis Pedidos para el cliente y en el detalle de órdenes del panel de Pedidos.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.PedidoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoHistorialRepository extends JpaRepository<PedidoHistorial, Integer> {

    // Trae en orden cronológico todas las etapas por las que ha pasado un pedido desde que se creó
    List<PedidoHistorial> findByIdPedidoOrderByCreadoEnAsc(Integer idPedido);
}
