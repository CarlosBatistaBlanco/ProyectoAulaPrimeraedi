package com.tecniservicios.tecnisecure.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.tecniservicios.tecnisecure.R
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.CarritoManager
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.Producto
import com.tecniservicios.tecnisecure.databinding.ActivityMainBinding
import com.tecniservicios.tecnisecure.ui.asistente.AsistenteIAActivity
import com.tecniservicios.tecnisecure.ui.auth.LoginActivity
import com.tecniservicios.tecnisecure.ui.carrito.CarritoActivity
import com.tecniservicios.tecnisecure.ui.catalogo.ProductoAdapter
import com.tecniservicios.tecnisecure.ui.catalogo.ProductoDetalleActivity
import com.tecniservicios.tecnisecure.ui.direcciones.DireccionesActivity
import com.tecniservicios.tecnisecure.ui.pedidos.MisPedidosActivity
import com.tecniservicios.tecnisecure.ui.tareas.TareasEmpleadoActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ProductoAdapter
    private var listaCompleta: List<Producto> = emptyList()
    private var categoriaActiva: String? = null
    private var textoBusquedaActivo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        configurarHeaderYRol()
        setupRecyclerView()
        setupListeners()
        resaltarBotonFiltro(binding.btnFiltroTodos)
        cargarProductos()
    }

    override fun onResume() {
        super.onResume()
        actualizarBadgeCarrito()
        // Mantener seleccionada la pestaña Catálogo al regresar de otras pantallas
        binding.bottomNav.menu.findItem(R.id.nav_catalogo)?.isChecked = true
    }

    private fun configurarHeaderYRol() {
        val rol = sessionManager.getUserRole()
        binding.tvUsuarioSaludo.text = "Hola, ${sessionManager.getUserName()} · Perfil ${rol.uppercase()}"

        // Si el usuario es EMPLEADO o ADMINISTRADOR, habilitar acceso a Tareas v2.0
        if (sessionManager.esEmpleado() || sessionManager.esAdmin()) {
            binding.btnAccesoTareas.visibility = View.VISIBLE
            binding.btnAccesoTareas.setOnClickListener {
                startActivity(Intent(this, TareasEmpleadoActivity::class.java))
            }
        } else {
            binding.btnAccesoTareas.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductoAdapter(
            lista = emptyList(),
            onProductoClick = { p ->
                val intent = Intent(this, ProductoDetalleActivity::class.java).apply {
                    putExtra(ProductoDetalleActivity.EXTRA_PRODUCTO, p)
                }
                startActivity(intent)
            },
            onAgregarClick = { p ->
                if ((p.stock ?: 0) <= 0) {
                    Toast.makeText(this, "Producto agotado temporalmente", Toast.LENGTH_SHORT).show()
                } else {
                    CarritoManager.agregar(p)
                    actualizarBadgeCarrito()
                    Toast.makeText(this, "Añadido al carrito: ${p.nombre}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvProductos.layoutManager = GridLayoutManager(this, 2)
        binding.rvProductos.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            cargarProductos()
        }

        binding.btnCarrito.setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }

        binding.btnAsistente.setOnClickListener {
            startActivity(Intent(this, AsistenteIAActivity::class.java))
        }

        binding.btnMenuOpciones.setOnClickListener {
            mostrarMenuOpciones()
        }

        // Filtros de las 4 categorías reales del backend
        binding.btnFiltroTodos.setOnClickListener {
            categoriaActiva = null
            resaltarBotonFiltro(binding.btnFiltroTodos)
            aplicarFiltrosLocales()
        }
        binding.btnFiltroCCTV.setOnClickListener {
            categoriaActiva = "Cámaras"
            resaltarBotonFiltro(binding.btnFiltroCCTV)
            aplicarFiltrosLocales()
        }
        binding.btnFiltroAlarmas.setOnClickListener {
            categoriaActiva = "Alarmas"
            resaltarBotonFiltro(binding.btnFiltroAlarmas)
            aplicarFiltrosLocales()
        }
        binding.btnFiltroControlAcceso.setOnClickListener {
            categoriaActiva = "Control de Acceso"
            resaltarBotonFiltro(binding.btnFiltroControlAcceso)
            aplicarFiltrosLocales()
        }
        binding.btnFiltroAccesorios.setOnClickListener {
            categoriaActiva = "Accesorios"
            resaltarBotonFiltro(binding.btnFiltroAccesorios)
            aplicarFiltrosLocales()
        }

        // Búsqueda en tiempo real combinada con categoría activa
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                textoBusquedaActivo = query?.trim().orEmpty()
                aplicarFiltrosLocales()
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                textoBusquedaActivo = newText?.trim().orEmpty()
                aplicarFiltrosLocales()
                return true
            }
        })

        // Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalogo -> {
                    cargarProductos()
                    true
                }
                R.id.nav_pedidos -> {
                    startActivity(Intent(this, MisPedidosActivity::class.java))
                    false
                }
                R.id.nav_direcciones -> {
                    startActivity(Intent(this, DireccionesActivity::class.java))
                    false
                }
                R.id.nav_asistente -> {
                    startActivity(Intent(this, AsistenteIAActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun resaltarBotonFiltro(activo: MaterialButton) {
        val botones = listOf(
            binding.btnFiltroTodos,
            binding.btnFiltroCCTV,
            binding.btnFiltroAlarmas,
            binding.btnFiltroControlAcceso,
            binding.btnFiltroAccesorios
        )
        val colorActivo = ContextCompat.getColorStateList(this, R.color.cian_brillante)
        val textoActivo = ContextCompat.getColor(this, R.color.azul_oscuro)
        val colorInactivo = ContextCompat.getColorStateList(this, R.color.cian_primario)
        val textoInactivo = ContextCompat.getColor(this, R.color.white)

        botones.forEach { btn ->
            if (btn.id == activo.id) {
                btn.backgroundTintList = colorActivo
                btn.setTextColor(textoActivo)
            } else {
                btn.backgroundTintList = colorInactivo
                btn.setTextColor(textoInactivo)
            }
        }
    }

    private fun cargarProductos() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@MainActivity)
                val response = api.getProductos(pagina = 0, tamanio = 100)
                if (response.isSuccessful && response.body() != null) {
                    listaCompleta = response.body()?.items ?: emptyList()
                    aplicarFiltrosLocales()
                } else if (response.code() == 401) {
                    Toast.makeText(this@MainActivity, "Sesión expirada. Por favor ingresa nuevamente.", Toast.LENGTH_SHORT).show()
                    sessionManager.logout()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "No se pudo cargar el catálogo (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun normalizarBusqueda(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val sinTildes = java.text.Normalizer.normalize(texto.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return sinTildes.replace(Regex("\\s+"), " ").trim()
    }

    private fun raizSingular(palabra: String): String {
        if (palabra.length > 5 && palabra.endsWith("ores")) return palabra.dropLast(2)
        if (palabra.length > 4 && palabra.endsWith("s") && !palabra.endsWith("ups")) return palabra.dropLast(1)
        return palabra
    }

    private fun aplicarFiltrosLocales() {
        val catNorm = normalizarBusqueda(categoriaActiva)
        val busqNorm = normalizarBusqueda(textoBusquedaActivo)
        val terminos = if (busqNorm.isEmpty()) emptyList() else {
            busqNorm.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.map { raizSingular(it) }
        }

        val filtrada = listaCompleta.filter { prod ->
            val prodCatNorm = normalizarBusqueda(prod.categoria)
            val coincideCat = catNorm.isEmpty() || prodCatNorm.contains(raizSingular(catNorm))

            val coincideTexto = if (busqNorm.isEmpty()) {
                true
            } else {
                val campos = normalizarBusqueda(
                    "${prod.nombre} ${prod.sku.orEmpty()} ${prod.marca.orEmpty()} ${prod.categoria.orEmpty()} ${prod.descripcion.orEmpty()}"
                )
                campos.contains(busqNorm) || terminos.all { campos.contains(it) }
            }

            coincideCat && coincideTexto
        }

        adapter.actualizarLista(filtrada)
        binding.tvSinResultados.visibility = if (filtrada.isEmpty() && listaCompleta.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun actualizarBadgeCarrito() {
        val cant = CarritoManager.getCantidadTotal()
        if (cant > 0) {
            binding.tvCarritoBadge.visibility = View.VISIBLE
            binding.tvCarritoBadge.text = cant.toString()
        } else {
            binding.tvCarritoBadge.visibility = View.GONE
        }
    }

    private fun mostrarMenuOpciones() {
        val esPersonal = sessionManager.esEmpleado() || sessionManager.esAdmin()
        val opciones = if (esPersonal) {
            arrayOf("Mis Direcciones", "Órdenes y Facturación", "Tareas Operativas v2.0", "Asistente Virtual IA", "Cerrar Sesión")
        } else {
            arrayOf("Mis Direcciones", "Mis Pedidos y Facturas", "Asistente Virtual IA", "Cerrar Sesión")
        }

        AlertDialog.Builder(this)
            .setTitle("${sessionManager.getUserName()} (${sessionManager.getUserRole()})")
            .setItems(opciones) { _, which ->
                if (esPersonal) {
                    when (which) {
                        0 -> startActivity(Intent(this, DireccionesActivity::class.java))
                        1 -> startActivity(Intent(this, MisPedidosActivity::class.java))
                        2 -> startActivity(Intent(this, TareasEmpleadoActivity::class.java))
                        3 -> startActivity(Intent(this, AsistenteIAActivity::class.java))
                        4 -> cerrarSesion()
                    }
                } else {
                    when (which) {
                        0 -> startActivity(Intent(this, DireccionesActivity::class.java))
                        1 -> startActivity(Intent(this, MisPedidosActivity::class.java))
                        2 -> startActivity(Intent(this, AsistenteIAActivity::class.java))
                        3 -> cerrarSesion()
                    }
                }
            }
            .show()
    }

    private fun cerrarSesion() {
        sessionManager.logout()
        CarritoManager.vaciar()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
