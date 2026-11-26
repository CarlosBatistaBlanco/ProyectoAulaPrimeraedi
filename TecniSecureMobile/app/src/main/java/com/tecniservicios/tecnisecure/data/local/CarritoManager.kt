package com.tecniservicios.tecnisecure.data.local

import com.tecniservicios.tecnisecure.data.model.ItemCarrito
import com.tecniservicios.tecnisecure.data.model.Producto

object CarritoManager {
    private val items = mutableListOf<ItemCarrito>()

    fun agregar(producto: Producto) {
        val existente = items.find { it.producto.idProducto == producto.idProducto }
        if (existente != null) {
            if (existente.cantidad < (producto.stock ?: 0)) {
                existente.cantidad += 1
            }
        } else {
            items.add(ItemCarrito(producto, 1))
        }
    }

    fun decrementar(producto: Producto) {
        val existente = items.find { it.producto.idProducto == producto.idProducto }
        if (existente != null) {
            if (existente.cantidad > 1) {
                existente.cantidad -= 1
            } else {
                items.remove(existente)
            }
        }
    }

    fun eliminar(producto: Producto) {
        items.removeAll { it.producto.idProducto == producto.idProducto }
    }

    fun getItems(): List<ItemCarrito> = items.toList()

    fun getTotal(): Double {
        return items.sumOf { it.producto.getPrecioFinal() * it.cantidad }
    }

    fun getCantidadTotal(): Int {
        return items.sumOf { it.cantidad }
    }

    fun vaciar() {
        items.clear()
    }
}
