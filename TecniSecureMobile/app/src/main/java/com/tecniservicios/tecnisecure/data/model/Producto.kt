package com.tecniservicios.tecnisecure.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Producto(
    @SerializedName("idProducto") val idProducto: Int = 0,
    @SerializedName("nombre") val nombre: String = "Equipo de Seguridad",
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("precio") val precio: Double = 0.0,
    @SerializedName("precioFinal") val precioFinal: Double? = null,
    @SerializedName("descuentoPct") val descuentoPct: Double? = null,
    @SerializedName("stock") val stock: Int? = 0,
    @SerializedName("stockBajo") val stockBajo: Boolean? = false,
    @SerializedName("imagen") val imagen: String? = null,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("marca") val marca: String? = null,
    @SerializedName("destacado") val destacado: Boolean? = false
) : Serializable {
    fun getPrecioFinal(): Double {
        return precioFinal ?: run {
            val desc = descuentoPct ?: 0.0
            if (desc > 0) precio * (1.0 - (desc / 100.0)) else precio
        }
    }
}

data class PageResponse<T>(
    @SerializedName("contenido") val contenido: List<T>? = null,
    @SerializedName("content") val content: List<T>? = null,
    @SerializedName("totalElementos") val totalElementos: Long? = 0L,
    @SerializedName("totalElements") val totalElements: Long? = 0L,
    @SerializedName("totalPaginas") val totalPaginas: Int? = 1,
    @SerializedName("totalPages") val totalPages: Int? = 1,
    @SerializedName("pagina") val pagina: Int? = 0,
    @SerializedName("tamanio") val tamanio: Int? = 20,
    @SerializedName("number") val number: Int? = 0
) : Serializable {
    val items: List<T> get() = contenido ?: content ?: emptyList()
}

data class ItemCarrito(
    val producto: Producto,
    var cantidad: Int
) : Serializable

data class ServicioTecnico(
    @SerializedName("idServicio") val idServicio: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("icono") val icono: String?
)
