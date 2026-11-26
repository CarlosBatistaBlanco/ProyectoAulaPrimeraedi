package com.tecniservicios.tecnisecure.ui.asistente

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tecniservicios.tecnisecure.R
import com.tecniservicios.tecnisecure.data.model.AsistenteMensaje
import com.tecniservicios.tecnisecure.databinding.ItemChatMensajeBinding

class ChatAdapter(
    private val mensajes: MutableList<AsistenteMensaje>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    fun agregarMensaje(m: AsistenteMensaje) {
        mensajes.add(m)
        notifyItemInserted(mensajes.size - 1)
    }

    fun limpiarMensajes() {
        mensajes.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMensajeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(mensajes[position])
    }

    override fun getItemCount(): Int = mensajes.size

    inner class ChatViewHolder(private val binding: ItemChatMensajeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(m: AsistenteMensaje) {
            binding.tvTextoMensaje.text = m.texto
            binding.tvHoraMensaje.text = m.hora

            val context = binding.root.context
            val params = binding.layoutMensaje.layoutParams as LinearLayout.LayoutParams

            if (m.esUsuario) {
                params.gravity = Gravity.END
                binding.layoutMensaje.backgroundTintList = ContextCompat.getColorStateList(context, R.color.cian_primario)
                binding.tvTextoMensaje.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.tvHoraMensaje.setTextColor(ContextCompat.getColor(context, R.color.borde_suave))
            } else {
                params.gravity = Gravity.START
                binding.layoutMensaje.backgroundTintList = ContextCompat.getColorStateList(context, R.color.card_bg)
                binding.tvTextoMensaje.setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
                binding.tvHoraMensaje.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario))
            }
            binding.layoutMensaje.layoutParams = params
        }
    }
}
