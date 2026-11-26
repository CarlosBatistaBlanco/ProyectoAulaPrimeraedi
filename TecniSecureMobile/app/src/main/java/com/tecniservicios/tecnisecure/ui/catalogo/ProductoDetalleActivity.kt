package com.tecniservicios.tecnisecure.ui.catalogo

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.tecniservicios.tecnisecure.R
import com.tecniservicios.tecnisecure.data.local.CarritoManager
import com.tecniservicios.tecnisecure.data.local.SessionManager
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

        @Suppress("DEPRECATION")
        producto = intent.getSerializableExtra(EXTRA_PRODUCTO) as? Producto
        if (producto == null) {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mostrarDetalles(producto!!)

        binding.btnDetalleAgregar.setOnClickListener { btn ->
            val p = producto ?: return@setOnClickListener
            btn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if ((p.stock ?: 0) <= 0) {
                Toast.makeText(this, "Producto agotado temporalmente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CarritoManager.agregar(p)
            binding.btnDetalleAgregar.isEnabled = false
            binding.btnDetalleAgregar.text = "Agregado al carrito"
            binding.btnDetalleAgregar.setIconResource(R.drawable.ic_check)
            binding.btnDetalleAgregar.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.verde_exito)

            btn.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btn.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                .start()

            btn.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    finish()
                }
            }, 550L)
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
                val baseUrl = SessionManager(this).getBaseUrl()
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
