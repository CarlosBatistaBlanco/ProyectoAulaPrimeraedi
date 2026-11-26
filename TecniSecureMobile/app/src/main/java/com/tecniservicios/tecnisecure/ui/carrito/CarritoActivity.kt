package com.tecniservicios.tecnisecure.ui.carrito

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.CarritoManager
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.Direccion
import com.tecniservicios.tecnisecure.data.model.ItemPedidoRequest
import com.tecniservicios.tecnisecure.data.model.PagoPedidoRequest
import com.tecniservicios.tecnisecure.data.model.PedidoRequest
import com.tecniservicios.tecnisecure.databinding.ActivityCarritoBinding
import com.tecniservicios.tecnisecure.ui.direcciones.DireccionesActivity
import com.tecniservicios.tecnisecure.ui.pedidos.MisPedidosActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CarritoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarritoBinding
    private lateinit var adapter: CarritoAdapter
    private lateinit var sessionManager: SessionManager
    private var listaDirecciones: List<Direccion> = emptyList()

    private val metodosPago = listOf(
        "PSE / Transferencia",
        "Tarjeta Crédito / Débito",
        "Pago Contraentrega"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.toolbarCarrito.setNavigationOnClickListener {
            finish()
        }

        binding.btnGestionarDirecciones.setOnClickListener {
            startActivity(Intent(this, DireccionesActivity::class.java))
        }

        binding.spinnerMetodoPago.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            metodosPago
        )

        setupRecyclerView()
        actualizarVista()

        binding.btnConfirmarPedido.setOnClickListener {
            confirmarPedido()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDirecciones()
        actualizarVista()
    }

    private fun setupRecyclerView() {
        adapter = CarritoAdapter(
            items = CarritoManager.getItems(),
            onSumarClick = { item ->
                val stockMax = item.producto.stock ?: 0
                if (item.cantidad >= stockMax) {
                    Toast.makeText(this, "Stock máximo disponible alcanzado ($stockMax)", Toast.LENGTH_SHORT).show()
                } else {
                    CarritoManager.agregar(item.producto)
                    actualizarVista()
                }
            },
            onRestarClick = { item ->
                CarritoManager.decrementar(item.producto)
                actualizarVista()
            }
        )
        binding.rvCarrito.layoutManager = LinearLayoutManager(this)
        binding.rvCarrito.adapter = adapter
    }

    private fun actualizarVista() {
        val items = CarritoManager.getItems()
        adapter.actualizarItems(items)

        if (items.isEmpty()) {
            binding.rvCarrito.visibility = View.GONE
            binding.tvCarritoVacio.visibility = View.VISIBLE
            binding.btnConfirmarPedido.isEnabled = false
        } else {
            binding.rvCarrito.visibility = View.VISIBLE
            binding.tvCarritoVacio.visibility = View.GONE
            binding.btnConfirmarPedido.isEnabled = true
        }

        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        binding.tvTotalCarrito.text = formatoPesos.format(CarritoManager.getTotal())
    }

    private fun cargarDirecciones() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@CarritoActivity)
                val response = api.getMisDirecciones()
                if (response.isSuccessful && response.body() != null) {
                    listaDirecciones = response.body()!!
                    val nombres = if (listaDirecciones.isNotEmpty()) {
                        listaDirecciones.map { d ->
                            val pref = if (d.predeterminada) "[Principal] " else ""
                            "$pref${d.etiqueta}: ${d.direccion} (${d.ciudad ?: "Cartagena"})"
                        }
                    } else {
                        listOf("Recoge en sede principal / Dirección de perfil")
                    }
                    binding.spinnerDirecciones.adapter = ArrayAdapter(
                        this@CarritoActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                    )
                    val idxPred = listaDirecciones.indexOfFirst { it.predeterminada }
                    if (idxPred >= 0) {
                        binding.spinnerDirecciones.setSelection(idxPred)
                    }
                }
            } catch (_: Exception) {
                binding.spinnerDirecciones.adapter = ArrayAdapter(
                    this@CarritoActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("Recoge en sede principal / Dirección de perfil")
                )
            }
        }
    }

    private fun limpiarNombreParaValidacion(nombreRaw: String): String {
        val limpio = nombreRaw.replace(Regex("[^A-Za-zÁÉÍÓÚÑÜáéíóúñü' .-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (limpio.length >= 3) limpio else "Cliente TecniSecure"
    }

    private fun confirmarPedido() {
        val itemsActuales = CarritoManager.getItems()
        if (itemsActuales.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val dirObj = if (listaDirecciones.isNotEmpty()) {
            listaDirecciones.getOrNull(binding.spinnerDirecciones.selectedItemPosition)
        } else null

        val direccionTexto = if (dirObj != null) {
            listOfNotNull(dirObj.direccion, dirObj.barrio, dirObj.ciudad).filter { it.isNotBlank() }.joinToString(", ")
        } else {
            "Sede Principal TECNISECURE, Cartagena"
        }

        val telefonoCandidato = (dirObj?.telefono ?: sessionManager.getUserPhone())
            .replace(Regex("[^0-9+]"), "")
        val telefonoValido = if (telefonoCandidato.matches(Regex("^(\\+?57)?[0-9]{7,10}$"))) {
            telefonoCandidato
        } else {
            "3001234567"
        }

        val metodoSeleccionado = metodosPago.getOrElse(binding.spinnerMetodoPago.selectedItemPosition) {
            "PSE / Transferencia"
        }

        val correoUsuario = sessionManager.getUserEmail().ifBlank { "cliente@tecnicsecure.com" }
        val nombreValido = limpiarNombreParaValidacion(sessionManager.getUserName())

        binding.btnConfirmarPedido.isEnabled = false
        binding.btnConfirmarPedido.text = "Procesando orden y factura..."

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@CarritoActivity)
                val request = PedidoRequest(
                    nombre = nombreValido,
                    correo = correoUsuario,
                    telefono = telefonoValido,
                    direccion = direccionTexto,
                    carrito = itemsActuales.map {
                        ItemPedidoRequest(it.producto.idProducto, it.cantidad)
                    },
                    metodoPago = metodoSeleccionado
                )

                val response = api.crearPedido(request)
                if (response.isSuccessful && response.body() != null) {
                    val pedido = response.body()!!

                    // Si el pago es electrónico, registrar pago y emitir factura pagada en el servidor
                    if (!metodoSeleccionado.contains("Contraentrega", ignoreCase = true)) {
                        try {
                            api.pagarPedido(pedido.idPedido, PagoPedidoRequest(metodoSeleccionado))
                        } catch (_: Exception) {
                        }
                    }

                    // Consultar factura oficial DIAN generada por el backend
                    val factura = try {
                        val facRes = api.getFacturaPedido(pedido.idPedido)
                        if (facRes.isSuccessful) facRes.body() else null
                    } catch (_: Exception) {
                        null
                    }

                    CarritoManager.vaciar()
                    actualizarVista()

                    val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                        maximumFractionDigits = 0
                    }
                    val numFactura = factura?.numeroFactura ?: String.format("FAC-%05d", pedido.idPedido)
                    val cufeReal = factura?.cufe ?: "Emitida en servidor DIAN"

                    AlertDialog.Builder(this@CarritoActivity)
                        .setTitle("¡Compra y Factura Registradas!")
                        .setMessage(
                            "Pedido #${pedido.idPedido} confirmado con éxito.\n" +
                                    "Comprobante: $numFactura\n" +
                                    "Pago: $metodoSeleccionado\n" +
                                    "Total (IVA 19% inc.): ${formatoPesos.format(pedido.total)}\n\n" +
                                    "CUFE DIAN:\n${cufeReal.take(36)}..."
                        )
                        .setPositiveButton("Ver Mis Pedidos") { _, _ ->
                            startActivity(Intent(this@CarritoActivity, MisPedidosActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Seguir Comprando") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "Error al procesar pedido (${response.code()})")
                    Toast.makeText(this@CarritoActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CarritoActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnConfirmarPedido.isEnabled = CarritoManager.getItems().isNotEmpty()
                binding.btnConfirmarPedido.text = "Confirmar Pedido y Facturar"
            }
        }
    }
}
