package com.tecniservicios.tecnisecure.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PedidoRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("carrito") val carrito: List<ItemPedidoRequest>,
    @SerializedName("metodoPago") val metodoPago: String? = null,
    @SerializedName("detallePago") val detallePago: String? = null
)

data class ItemPedidoRequest(
    @SerializedName("idProducto") val idProducto: Int,
    @SerializedName("cantidad") val cantidad: Int
)

data class PedidoResponse(
    @SerializedName("idPedido") val idPedido: Int = 0,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("estado") val estado: String = "pendiente",
    @SerializedName(value = "creadoEn", alternate = ["fecha"]) val creadoEn: String? = null,
    @SerializedName("idCliente") val idCliente: Int? = null,
    @SerializedName("clienteNombre") val clienteNombre: String? = null,
    @SerializedName("clienteCorreo") val clienteCorreo: String? = null,
    @SerializedName("clienteTelefono") val clienteTelefono: String? = null,
    @SerializedName(value = "clienteDireccion", alternate = ["direccionEntrega"]) val clienteDireccion: String? = null,
    @SerializedName("metodoPago") val metodoPago: String? = null,
    @SerializedName("numeroFactura") val numeroFactura: String? = null,
    @SerializedName("cufe") val cufe: String? = null,
    @SerializedName("detalles") val detalles: List<DetallePedidoResponse>? = null
) : Serializable

data class DetallePedidoResponse(
    @SerializedName("idDetalle") val idDetalle: Int? = null,
    @SerializedName("idProducto") val idProducto: Int? = null,
    @SerializedName(value = "producto", alternate = ["nombreProducto"]) val producto: String? = null,
    @SerializedName("cantidad") val cantidad: Int = 1,
    @SerializedName("precioUnitario") val precioUnitario: Double = 0.0,
    @SerializedName("subtotal") val subtotal: Double? = null
) : Serializable

data class FacturaDetalleResponse(
    @SerializedName("idFactura") val idFactura: Int? = null,
    @SerializedName("idPedido") val idPedido: Int = 0,
    @SerializedName("numeroFactura") val numeroFactura: String? = null,
    @SerializedName("cufe") val cufe: String? = null,
    @SerializedName("estadoFactura") val estadoFactura: String? = null,
    @SerializedName("estadoPedido") val estadoPedido: String? = null,
    @SerializedName("fechaEmision") val fechaEmision: String? = null,
    @SerializedName("emisorRazonSocial") val emisorRazonSocial: String? = null,
    @SerializedName("emisorNit") val emisorNit: String? = null,
    @SerializedName("emisorRegimen") val emisorRegimen: String? = null,
    @SerializedName("resolucionDian") val resolucionDian: String? = null,
    @SerializedName("clienteNombre") val clienteNombre: String? = null,
    @SerializedName("clienteCorreo") val clienteCorreo: String? = null,
    @SerializedName("clienteTelefono") val clienteTelefono: String? = null,
    @SerializedName("clienteDireccion") val clienteDireccion: String? = null,
    @SerializedName("metodoPago") val metodoPago: String? = null,
    @SerializedName("subtotalBase") val subtotalBase: Double? = null,
    @SerializedName("ivaPct") val ivaPct: Double? = null,
    @SerializedName("ivaValor") val ivaValor: Double? = null,
    @SerializedName("descuento") val descuento: Double? = null,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("items") val items: List<DetallePedidoResponse>? = null
) : Serializable

data class PagoPedidoRequest(
    @SerializedName("metodo") val metodo: String
)

data class Direccion(
    @SerializedName("idDireccion") val idDireccion: Int,
    @SerializedName("etiqueta") val etiqueta: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("ciudad") val ciudad: String?,
    @SerializedName("barrio") val barrio: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("nombreRecibe") val nombreRecibe: String? = null,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("predeterminada") val predeterminada: Boolean = false
) : Serializable

data class DireccionRequest(
    @SerializedName("etiqueta") val etiqueta: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("ciudad") val ciudad: String? = null,
    @SerializedName("barrio") val barrio: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("nombreRecibe") val nombreRecibe: String? = null,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("predeterminada") val predeterminada: Boolean = false
)
