/*
 * Este archivo sirve como el buscador que consulta y guarda los comprobantes de
 * factura generados por las compras. Se utiliza en la sección de Facturación del panel,
 * al emitir nuevos comprobantes y cuando un cliente o empleado descarga el recibo de un pedido.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    // Encuentra el comprobante de factura que corresponde a un pedido específico
    Optional<Factura> findByIdPedido(Integer idPedido);

    // Comprueba si un número de factura ya fue utilizado para evitar comprobantes repetidos
    boolean existsByNumeroFactura(String numeroFactura);
}
