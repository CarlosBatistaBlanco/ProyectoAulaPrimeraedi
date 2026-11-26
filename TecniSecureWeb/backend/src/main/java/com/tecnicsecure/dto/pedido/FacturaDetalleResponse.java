/*
 * Este archivo reúne todos los datos necesarios para mostrar o imprimir la factura
 * electrónica formal de una compra en la pantalla de pedidos. Incluye la información
 * fiscal de TecniServicios, los datos del comprador, el desglose del IVA y los artículos comprados.
 */
package com.tecnicsecure.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaDetalleResponse(
        // Identificadores de la factura y del pedido, número consecutivo y código único de validación tributaria
        Integer idFactura,
        Integer idPedido,
        String numeroFactura,
        String cufe,
        // Estado actual de la factura, estado del pedido y fecha en que se emitió
        String estadoFactura,
        String estadoPedido,
        LocalDateTime fechaEmision,
        // Datos legales y tributarios de TecniServicios como empresa emisora
        String emisorRazonSocial,
        String emisorNit,
        String emisorRegimen,
        String resolucionDian,
        // Datos de contacto y dirección de entrega del comprador
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        String clienteDireccion,
        // Medio de pago utilizado y desglose de valores: base, IVA, descuentos y total final
        String metodoPago,
        BigDecimal subtotalBase,
        BigDecimal ivaPct,
        BigDecimal ivaValor,
        BigDecimal descuento,
        BigDecimal total,
        // Lista detallada de cada producto adquirido en esta factura
        List<PedidoResponse.DetalleResponse> items
) {
}
