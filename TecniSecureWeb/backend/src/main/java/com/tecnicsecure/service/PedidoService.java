/*
 * Este archivo define las acciones principales de las compras en la tienda: confirmar un pedido
 * desde el carrito, consultar el listado, actualizar su estado de entrega y ver su historial.
 * Su trabajo se nota en el carrito de compras, en "Mis pedidos" del cliente y en el panel de ventas.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.pedido.PedidoRequest;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.PedidoHistorial;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PedidoService {

    // Procesa una nueva compra, calcula los precios reales y descuenta las unidades de bodega.
    PedidoResponse registrar(PedidoRequest request);

    // Muestra los pedidos divididos en paginas, con opcion de filtrar por cliente o por estado.
    PageResponse<PedidoResponse> listar(Integer idCliente, String estado, Pageable pageable);

    // Consulta el detalle completo de un pedido verificando que la persona tenga permiso para verlo.
    PedidoResponse obtener(Integer idPedido);

    // Cambia el estado de un pedido (por ejemplo a enviado, entregado o cancelado) y avisa al cliente.
    PedidoResponse cambiarEstado(Integer idPedido, String nuevoEstado, String observacion, Integer idUsuario);

    // Muestra la linea de tiempo con cada cambio de estado por el que ha pasado un pedido.
    List<PedidoHistorial> historial(Integer idPedido);

    // Muestra unicamente los pedidos que pertenecen al cliente que tiene la sesion abierta.
    PageResponse<PedidoResponse> misPedidos(Pageable pageable);
}
