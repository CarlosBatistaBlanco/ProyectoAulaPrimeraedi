/*
 * Este archivo prepara toda la información visible de un pedido para mostrarla en la
 * sección "Mis pedidos" del cliente y en la pantalla de gestión de compras del equipo
 * interno. Incluye estado, total, dirección de entrega, datos del pago y cada producto.
 */
package com.tecnicsecure.dto.pedido;

import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.entity.PedidoDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        // Número del pedido, estado actual, valor total y fecha en que se realizó la compra
        Integer idPedido,
        String estado,
        BigDecimal total,
        LocalDateTime creadoEn,
        // Información de contacto y dirección de entrega del comprador
        Integer idCliente,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        String clienteDireccion,
        // Ubicación en el mapa y detalles de la forma de pago elegida
        Double latitud,
        Double longitud,
        String metodoPago,
        String detallePago,
        // Lista de artículos incluidos dentro de esta compra
        List<DetalleResponse> detalles
) {

    // Información individual de cada producto comprado dentro del pedido
    public record DetalleResponse(
            Integer idDetalle,
            Integer idProducto,
            String producto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        // Toma una línea guardada del pedido y la prepara con el nombre del producto y sus valores
        public static DetalleResponse de(PedidoDetalle d) {
            return new DetalleResponse(d.getIdDetalle(), d.getProducto().getIdProducto(),
                    d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
        }
    }

    // Toma un pedido guardado y arma su resumen completo listo para mostrarlo en pantalla
    public static PedidoResponse de(Pedido p) {
        var c = p.getCliente();
        String dir = (p.getDireccionEntrega() != null && !p.getDireccionEntrega().isBlank())
                ? p.getDireccionEntrega()
                : (c != null ? c.getDireccion() : null);
        return new PedidoResponse(
                p.getIdPedido(),
                p.getEstado(),
                p.getTotal(),
                p.getCreadoEn(),
                c != null ? c.getIdCliente() : null,
                c != null ? c.getNombre() : null,
                c != null ? c.getCorreo() : null,
                c != null ? c.getTelefono() : null,
                dir,
                p.getLatitud(),
                p.getLongitud(),
                p.getMetodoPago(),
                p.getDetallePago(),
                p.getDetalles() == null ? List.of()
                        : p.getDetalles().stream().map(DetalleResponse::de).toList());
    }
}
