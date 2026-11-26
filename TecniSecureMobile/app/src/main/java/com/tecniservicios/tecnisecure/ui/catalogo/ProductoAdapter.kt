package com.tecniservicios.tecnisecure.ui.catalogo

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecniservicios.tecnisecure.R
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.Producto
import com.tecniservicios.tecnisecure.databinding.ItemProductoBinding
import java.text.NumberFormat
import java.util.Locale

class ProductoAdapter(
    private var lista: List<Producto>,
    private val onProductoClick: (Producto) -> Unit,
    private val onAgregarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    fun actualizarLista(nuevaLista: List<Producto>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    inner class ProductoViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        private var resetRunnable: Runnable? = null

        fun bind(producto: Producto) {
            val context = binding.root.context
            resetRunnable?.let { binding.btnAgregarCarrito.removeCallbacks(it) }
            restaurarBotonAgregar(context)

            val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatoPesos.maximumFractionDigits = 0

            binding.tvNombreProducto.text = producto.nombre
            binding.tvCategoriaMarca.text = "${producto.categoria ?: "Seguridad"} · ${producto.marca ?: "Genérico"}"
            binding.tvPrecio.text = formatoPesos.format(producto.getPrecioFinal())
            binding.tvStock.text = "Stock: ${producto.stock}"

            // Badge descuento
            val desc = producto.descuentoPct ?: 0.0
            if (desc > 0) {
                binding.tvBadgeDescuento.visibility = View.VISIBLE
                binding.tvBadgeDescuento.text = "-${desc.toInt()}%"
            } else {
                binding.tvBadgeDescuento.visibility = View.GONE
            }

            // Imagen del producto
            val urlImagen = if (!producto.imagen.isNullOrEmpty()) {
                if (producto.imagen.startsWith("http://") || producto.imagen.startsWith("https://")) {
                    producto.imagen
                } else {
                    val baseUrl = SessionManager(context).getBaseUrl()
                    "${baseUrl.trimEnd('/')}/${producto.imagen.removePrefix("/")}"
                }
            } else null

            if (!urlImagen.isNullOrEmpty()) {
                Glide.with(context)
                    .load(urlImagen)
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivProducto)
            } else {
                binding.ivProducto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener {
                onProductoClick(producto)
            }

            binding.btnAgregarCarrito.setOnClickListener { btn ->
                btn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if ((producto.stock ?: 0) > 0) {
                    mostrarConfirmacionEnBoton(context)
                } else {
                    btn.animate()
                        .translationX(-8f)
                        .setDuration(60)
                        .withEndAction {
                            btn.animate().translationX(0f).setDuration(80).start()
                        }
                        .start()
                }
                onAgregarClick(producto)
            }
        }

        private fun mostrarConfirmacionEnBoton(context: android.content.Context) {
            val btn = binding.btnAgregarCarrito
            resetRunnable?.let { btn.removeCallbacks(it) }

            btn.backgroundTintList = ContextCompat.getColorStateList(context, R.color.verde_exito)
            btn.setIconResource(R.drawable.ic_check)

            btn.animate().cancel()
            btn.scaleX = 0.82f
            btn.scaleY = 0.82f
            btn.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(140)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    btn.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()

            val runnable = Runnable { restaurarBotonAgregar(context) }
            resetRunnable = runnable
            btn.postDelayed(runnable, 1100L)
        }

        private fun restaurarBotonAgregar(context: android.content.Context) {
            binding.btnAgregarCarrito.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.cian_primario)
            binding.btnAgregarCarrito.setIconResource(android.R.drawable.ic_input_add)
            binding.btnAgregarCarrito.scaleX = 1f
            binding.btnAgregarCarrito.scaleY = 1f
        }
    }
}
