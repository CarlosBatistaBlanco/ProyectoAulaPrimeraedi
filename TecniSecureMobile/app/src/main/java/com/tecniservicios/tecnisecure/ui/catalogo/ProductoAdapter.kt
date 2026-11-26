package com.tecniservicios.tecnisecure.ui.catalogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecniservicios.tecnisecure.R
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
        fun bind(producto: Producto) {
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
                    val baseUrl = com.tecniservicios.tecnisecure.data.local.SessionManager(binding.root.context).getBaseUrl()
                    "${baseUrl.trimEnd('/')}/${producto.imagen.removePrefix("/")}"
                }
            } else null

            if (!urlImagen.isNullOrEmpty()) {
                Glide.with(binding.root.context)
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

            binding.btnAgregarCarrito.setOnClickListener {
                onAgregarClick(producto)
            }
        }
    }
}
