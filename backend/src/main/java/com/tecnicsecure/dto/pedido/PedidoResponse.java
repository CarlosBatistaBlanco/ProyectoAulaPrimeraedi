package com.tecnicsecure.dto.pedido;

import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.entity.PedidoDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Integer idPedido,
        String estado,
        BigDecimal total,
        LocalDateTime creadoEn,
        Integer idCliente,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        String clienteDireccion,
        Double latitud,
        Double longitud,
        String metodoPago,
        String detallePago,
        List<DetalleResponse> detalles
) {

    public record DetalleResponse(
            Integer idDetalle,
            Integer idProducto,
            String producto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        public static DetalleResponse de(PedidoDetalle d) {
            return new DetalleResponse(d.getIdDetalle(), d.getProducto().getIdProducto(),
                    d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
        }
    }

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
