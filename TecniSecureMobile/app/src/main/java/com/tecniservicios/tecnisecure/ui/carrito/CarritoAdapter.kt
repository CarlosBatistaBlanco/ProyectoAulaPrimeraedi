package com.tecniservicios.tecnisecure.ui.carrito

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecniservicios.tecnisecure.data.model.ItemCarrito
import com.tecniservicios.tecnisecure.databinding.ItemCarritoBinding
import java.text.NumberFormat
import java.util.Locale

class CarritoAdapter(
    private var items: List<ItemCarrito>,
    private val onSumarClick: (ItemCarrito) -> Unit,
    private val onRestarClick: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    fun actualizarItems(nuevos: List<ItemCarrito>) {
        items = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CarritoViewHolder(private val binding: ItemCarritoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ItemCarrito) {
            val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatoPesos.maximumFractionDigits = 0

            binding.tvItemCarritoNombre.text = item.producto.nombre
            binding.tvItemCarritoPrecio.text = "${formatoPesos.format(item.producto.getPrecioFinal())} c/u"
            binding.tvCantidad.text = item.cantidad.toString()

            val imagenRaw = item.producto.imagen
            val urlImagen = if (!imagenRaw.isNullOrEmpty()) {
                if (imagenRaw.startsWith("http://") || imagenRaw.startsWith("https://")) {
                    imagenRaw
                } else {
                    val baseUrl = com.tecniservicios.tecnisecure.data.local.SessionManager(binding.root.context).getBaseUrl()
                    "${baseUrl.trimEnd('/')}/${imagenRaw.removePrefix("/")}"
                }
            } else null

            if (!urlImagen.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(urlImagen)
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivItemCarritoFoto)
            } else {
                binding.ivItemCarritoFoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.btnMasCantidad.setOnClickListener {
                onSumarClick(item)
            }

            binding.btnMenosCantidad.setOnClickListener {
                onRestarClick(item)
            }
        }
    }
}
