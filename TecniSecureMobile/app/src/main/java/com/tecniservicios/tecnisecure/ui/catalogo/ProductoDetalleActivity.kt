package com.tecniservicios.tecnisecure.ui.catalogo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.tecniservicios.tecnisecure.data.local.CarritoManager
import com.tecniservicios.tecnisecure.data.model.Producto
import com.tecniservicios.tecnisecure.databinding.ActivityProductoDetalleBinding
import java.text.NumberFormat
import java.util.Locale

class ProductoDetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductoDetalleBinding
    private var producto: Producto? = null

    companion object {
        const val EXTRA_PRODUCTO = "extra_producto"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductoDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDetalle.setNavigationOnClickListener {
            finish()
        }

        producto = intent.getSerializableExtra(EXTRA_PRODUCTO) as? Producto
        if (producto == null) {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mostrarDetalles(producto!!)

        binding.btnDetalleAgregar.setOnClickListener {
            CarritoManager.agregar(producto!!)
            Toast.makeText(this, "¡${producto!!.nombre} añadido al carrito!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun mostrarDetalles(p: Producto) {
        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatoPesos.maximumFractionDigits = 0

        binding.tvDetalleNombre.text = p.nombre
        binding.tvDetalleCategoria.text = "${p.categoria ?: "Seguridad"} · ${p.marca ?: "Marca Oficial"}"
        binding.tvDetalleSku.text = "SKU: ${p.sku ?: "TEC-${p.idProducto}"}"
        binding.tvDetallePrecio.text = formatoPesos.format(p.getPrecioFinal())
        binding.tvDetalleStock.text = "Stock disponible: ${p.stock} unidades"
        binding.tvDetalleDescripcion.text = p.descripcion ?: "Equipo de seguridad electrónica de alta fidelidad, certificado para entornos residenciales y corporativos."

        val urlImagen = if (!p.imagen.isNullOrEmpty()) {
            if (p.imagen.startsWith("http://") || p.imagen.startsWith("https://")) {
                p.imagen
            } else {
                val baseUrl = com.tecniservicios.tecnisecure.data.local.SessionManager(this).getBaseUrl()
                "${baseUrl.trimEnd('/')}/${p.imagen.removePrefix("/")}"
            }
        } else null

        if (!urlImagen.isNullOrEmpty()) {
            Glide.with(this)
                .load(urlImagen)
                .fitCenter()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivDetalleFoto)
        }
    }
}
