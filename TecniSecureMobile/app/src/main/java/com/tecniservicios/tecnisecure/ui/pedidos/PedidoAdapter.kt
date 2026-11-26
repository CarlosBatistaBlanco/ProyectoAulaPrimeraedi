package com.tecniservicios.tecnisecure.ui.pedidos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.model.PedidoResponse
import com.tecniservicios.tecnisecure.databinding.ItemPedidoBinding
import java.text.NumberFormat
import java.util.Locale

class PedidoAdapter(
    private var lista: List<PedidoResponse>,
    private val mostrarCliente: Boolean = false,
    private val onVerFacturaClick: (PedidoResponse) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    fun actualizarLista(nueva: List<PedidoResponse>) {
        lista = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    inner class PedidoViewHolder(private val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pedido: PedidoResponse) {
            val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                maximumFractionDigits = 0
            }

            val clienteSufijo = if (mostrarCliente && !pedido.clienteNombre.isNullOrBlank()) {
                " · ${pedido.clienteNombre}"
            } else ""
            binding.tvNumeroPedido.text = "Pedido #${pedido.idPedido}$clienteSufijo"

            val estadoUpper = pedido.estado.uppercase()
            binding.tvEstadoPedido.text = estadoUpper
            when (pedido.estado.lowercase()) {
                "cancelado" -> {
                    binding.tvEstadoPedido.setTextColor(Color.parseColor("#B91C1C"))
                    binding.tvEstadoPedido.setBackgroundColor(Color.parseColor("#FEE2E2"))
                }
                "pendiente" -> {
                    binding.tvEstadoPedido.setTextColor(Color.parseColor("#B45309"))
                    binding.tvEstadoPedido.setBackgroundColor(Color.parseColor("#FEF3C7"))
                }
                else -> {
                    binding.tvEstadoPedido.setTextColor(Color.parseColor("#059669"))
                    binding.tvEstadoPedido.setBackgroundColor(Color.parseColor("#D1FAE5"))
                }
            }

            val pagoTxt = if (!pedido.metodoPago.isNullOrBlank()) " · ${pedido.metodoPago}" else ""
            binding.tvFechaPedido.text = "Fecha: ${ApiErrorUtils.formatearFecha(pedido.creadoEn)}$pagoTxt"

            val cantItems = pedido.detalles?.sumOf { it.cantidad } ?: 0
            val resumenItems = if (cantItems > 0) " ($cantItems ud.)" else ""
            binding.tvDireccionPedido.text = "Entrega: ${pedido.clienteDireccion ?: "Sede Principal"}$resumenItems"
            binding.tvTotalPedido.text = formatoPesos.format(pedido.total)

            val numFac = pedido.numeroFactura ?: String.format("FAC-%05d", pedido.idPedido)
            binding.tvFacturaCufe.text = "Factura Electrónica $numFac · DIAN"

            binding.btnVerFactura.setOnClickListener {
                onVerFacturaClick(pedido)
            }
        }
    }
}
