package com.tecnicsecure.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Factura electronica formal lista para impresion y validacion tributaria sin costos internos. */
public record FacturaDetalleResponse(
        Integer idFactura,
        Integer idPedido,
        String numeroFactura,
        String cufe,
        String estadoFactura,
        String estadoPedido,
        LocalDateTime fechaEmision,
        String emisorRazonSocial,
        String emisorNit,
        String emisorRegimen,
        String resolucionDian,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        String clienteDireccion,
        String metodoPago,
        BigDecimal subtotalBase,
        BigDecimal ivaPct,
        BigDecimal ivaValor,
        BigDecimal descuento,
        BigDecimal total,
        List<PedidoResponse.DetalleResponse> items
) {
}
