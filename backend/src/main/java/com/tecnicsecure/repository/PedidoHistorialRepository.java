package com.tecnicsecure.repository;

import com.tecnicsecure.entity.PedidoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoHistorialRepository extends JpaRepository<PedidoHistorial, Integer> {

    List<PedidoHistorial> findByIdPedidoOrderByCreadoEnAsc(Integer idPedido);
}
