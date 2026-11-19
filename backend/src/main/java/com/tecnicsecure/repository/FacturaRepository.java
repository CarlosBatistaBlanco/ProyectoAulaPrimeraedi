package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    Optional<Factura> findByIdPedido(Integer idPedido);

    boolean existsByNumeroFactura(String numeroFactura);
}
