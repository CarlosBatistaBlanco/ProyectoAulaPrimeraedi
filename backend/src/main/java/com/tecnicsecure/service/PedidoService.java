package com.tecnicsecure.service;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.pedido.PedidoRequest;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.PedidoHistorial;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PedidoService {

    PedidoResponse registrar(PedidoRequest request);

    PageResponse<PedidoResponse> listar(Integer idCliente, String estado, Pageable pageable);

    PedidoResponse obtener(Integer idPedido);

    PedidoResponse cambiarEstado(Integer idPedido, String nuevoEstado, String observacion, Integer idUsuario);

    List<PedidoHistorial> historial(Integer idPedido);

    /** Pedidos del cliente autenticado (aislamiento por propietario). */
    PageResponse<PedidoResponse> misPedidos(Pageable pageable);
}
