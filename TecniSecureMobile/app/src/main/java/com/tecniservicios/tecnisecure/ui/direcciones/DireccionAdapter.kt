package com.tecniservicios.tecnisecure.ui.direcciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tecniservicios.tecnisecure.data.model.Direccion
import com.tecniservicios.tecnisecure.databinding.ItemDireccionBinding

class DireccionAdapter(
    private var lista: List<Direccion>,
    private val onItemClick: (Direccion) -> Unit,
    private val onEliminarClick: (Direccion) -> Unit
) : RecyclerView.Adapter<DireccionAdapter.DireccionViewHolder>() {

    fun actualizarLista(nueva: List<Direccion>) {
        lista = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DireccionViewHolder {
        val binding = ItemDireccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DireccionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DireccionViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    inner class DireccionViewHolder(private val binding: ItemDireccionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(d: Direccion) {
            binding.tvDirEtiqueta.text = d.etiqueta
            val partes = listOfNotNull(
                d.direccion.takeIf { it.isNotBlank() },
                d.barrio?.takeIf { it.isNotBlank() },
                (d.ciudad ?: "Cartagena").takeIf { it.isNotBlank() }
            )
            binding.tvDirDetalle.text = partes.joinToString(", ")
            binding.tvDirTelefono.text = if (!d.telefono.isNullOrBlank()) {
                "Contacto: ${d.telefono}"
            } else {
                "Toca para editar o marcar como predeterminada"
            }

            binding.tvDirPredeterminada.visibility = if (d.predeterminada) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClick(d)
            }

            binding.btnEliminarDireccion.setOnClickListener {
                onEliminarClick(d)
            }
        }
    }
}
