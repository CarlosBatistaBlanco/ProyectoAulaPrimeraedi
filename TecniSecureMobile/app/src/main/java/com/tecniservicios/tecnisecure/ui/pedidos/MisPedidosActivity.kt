package com.tecniservicios.tecnisecure.ui.pedidos

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.PagoPedidoRequest
import com.tecniservicios.tecnisecure.data.model.PedidoResponse
import com.tecniservicios.tecnisecure.databinding.ActivityMisPedidosBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MisPedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisPedidosBinding
    private lateinit var adapter: PedidoAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.esAdmin() || sessionManager.esEmpleado()) {
            binding.toolbarPedidos.title = "Órdenes y Facturación"
        } else {
            binding.toolbarPedidos.title = "Mis Pedidos y Facturas"
        }

        binding.toolbarPedidos.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        cargarPedidos()

        binding.swipePedidos.setOnRefreshListener {
            cargarPedidos()
        }
    }

    private fun setupRecyclerView() {
        adapter = PedidoAdapter(
            lista = emptyList(),
            mostrarCliente = sessionManager.esAdmin() || sessionManager.esEmpleado(),
            onVerFacturaClick = { pedido ->
                consultarYMostrarFactura(pedido)
            }
        )
        binding.rvMisPedidos.layoutManager = LinearLayoutManager(this)
        binding.rvMisPedidos.adapter = adapter
    }

    private fun cargarPedidos() {
        binding.swipePedidos.isRefreshing = true
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@MisPedidosActivity)
                val esPersonal = sessionManager.esAdmin() || sessionManager.esEmpleado()
                val response = if (esPersonal) {
                    api.getTodosLosPedidos(pagina = 0, tamanio = 50)
                } else {
                    api.getMisPedidos(pagina = 0, tamanio = 50)
                }

                if (response.isSuccessful && response.body() != null) {
                    val pedidos = response.body()?.items ?: emptyList()
                    adapter.actualizarLista(pedidos)
                    binding.tvPedidosVacio.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
                } else if (response.code() == 404) {
                    adapter.actualizarLista(emptyList())
                    binding.tvPedidosVacio.visibility = View.VISIBLE
                } else if (response.code() == 401) {
                    Toast.makeText(this@MisPedidosActivity, "Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "No se pudieron cargar los pedidos (${response.code()})")
                    Toast.makeText(this@MisPedidosActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisPedidosActivity, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipePedidos.isRefreshing = false
            }
        }
    }

    private fun consultarYMostrarFactura(pedido: PedidoResponse) {
        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@MisPedidosActivity)
                val response = api.getFacturaPedido(pedido.idPedido)
                if (response.isSuccessful && response.body() != null) {
                    val fac = response.body()!!
                    val lineas = (fac.items ?: pedido.detalles)?.joinToString("\n") { det ->
                        val sub = det.subtotal ?: (det.precioUnitario * det.cantidad)
                        "• ${det.producto ?: "Equipo"} x${det.cantidad} — ${formatoPesos.format(sub)}"
                    } ?: "• Equipos de seguridad electrónica"

                    val mensaje = buildString {
                        appendLine(fac.emisorRazonSocial ?: "TECNISERVICIOS & TECNOLOGÍA TECNISECURE S.A.S.")
                        appendLine("NIT: ${fac.emisorNit ?: "901.456.789-2"} · ${fac.emisorRegimen ?: "Régimen Común"}")
                        appendLine("Factura: ${fac.numeroFactura ?: "FAC-${pedido.idPedido}"} (${(fac.estadoFactura ?: "emitida").uppercase()})")
                        appendLine("Fecha: ${ApiErrorUtils.formatearFecha(fac.fechaEmision ?: pedido.creadoEn)}")
                        appendLine()
                        appendLine("Comprador: ${fac.clienteNombre ?: pedido.clienteNombre ?: sessionManager.getUserName()}")
                        appendLine("Dirección: ${fac.clienteDireccion ?: pedido.clienteDireccion ?: "Sede Principal"}")
                        appendLine("Método de pago: ${fac.metodoPago ?: pedido.metodoPago ?: "Por confirmar"}")
                        appendLine()
                        appendLine("Detalle de compra:")
                        appendLine(lineas)
                        appendLine()
                        if (fac.subtotalBase != null && fac.ivaValor != null) {
                            appendLine("Base gravable: ${formatoPesos.format(fac.subtotalBase)}")
                            appendLine("IVA (${fac.ivaPct?.toInt() ?: 19}%): ${formatoPesos.format(fac.ivaValor)}")
                        }
                        appendLine("TOTAL: ${formatoPesos.format(fac.total)}")
                        appendLine()
                        append("CUFE DIAN:\n${fac.cufe ?: "Verificado"}")
                    }

                    val builder = AlertDialog.Builder(this@MisPedidosActivity)
                        .setTitle("Factura Electrónica ${fac.numeroFactura ?: "#${pedido.idPedido}"}")
                        .setMessage(mensaje)
                        .setPositiveButton("Cerrar", null)

                    if (pedido.estado.equals("pendiente", ignoreCase = true) &&
                        !fac.estadoFactura.equals("pagada", ignoreCase = true)
                    ) {
                        builder.setNeutralButton("Registrar Pago") { _, _ ->
                            registrarPagoPedido(pedido)
                        }
                    }

                    builder.show()
                } else {
                    mostrarDialogoFacturaLocal(pedido, formatoPesos)
                }
            } catch (_: Exception) {
                mostrarDialogoFacturaLocal(pedido, formatoPesos)
            }
        }
    }

    private fun registrarPagoPedido(pedido: PedidoResponse) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@MisPedidosActivity)
                val res = api.pagarPedido(
                    pedido.idPedido,
                    PagoPedidoRequest(pedido.metodoPago ?: "PSE / Transferencia")
                )
                if (res.isSuccessful) {
                    Toast.makeText(this@MisPedidosActivity, "Pago aprobado y factura confirmada", Toast.LENGTH_LONG).show()
                    cargarPedidos()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(res, "No se pudo registrar el pago")
                    Toast.makeText(this@MisPedidosActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisPedidosActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoFacturaLocal(pedido: PedidoResponse, formatoPesos: NumberFormat) {
        val detallesTexto = pedido.detalles?.joinToString("\n") {
            "• ${it.producto ?: "Equipo"} x${it.cantidad} — ${formatoPesos.format(it.precioUnitario)}"
        } ?: "• Compra de productos de seguridad electrónica"

        AlertDialog.Builder(this)
            .setTitle("Factura — Pedido #${pedido.idPedido}")
            .setMessage(
                "TECNISERVICIOS & TECNOLOGÍA TECNISECURE S.A.S.\n" +
                        "NIT: 901.456.789-2\n" +
                        "Estado: ${pedido.estado.uppercase()}\n" +
                        "Total (IVA 19% inc.): ${formatoPesos.format(pedido.total)}\n\n" +
                        "Detalle:\n$detallesTexto"
            )
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
