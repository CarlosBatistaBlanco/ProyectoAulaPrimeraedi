package com.tecniservicios.tecnisecure.ui.direcciones

import android.os.Bundle
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.model.Direccion
import com.tecniservicios.tecnisecure.data.model.DireccionRequest
import com.tecniservicios.tecnisecure.databinding.ActivityDireccionesBinding
import kotlinx.coroutines.launch

class DireccionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDireccionesBinding
    private lateinit var adapter: DireccionAdapter
    private var cantidadDirecciones = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDireccionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDirecciones.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        cargarDirecciones()

        binding.btnNuevaDireccion.setOnClickListener {
            if (cantidadDirecciones >= 15) {
                AlertDialog.Builder(this)
                    .setTitle("Límite de Direcciones Alcanzado")
                    .setMessage("Por políticas de seguridad y almacenamiento, cada usuario puede registrar un máximo de 15 direcciones de entrega.")
                    .setPositiveButton("Entendido", null)
                    .show()
            } else {
                mostrarDialogoFormularioDireccion(null)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DireccionAdapter(
            lista = emptyList(),
            onItemClick = { d ->
                mostrarOpcionesDireccion(d)
            },
            onEliminarClick = { d ->
                eliminarDireccion(d)
            }
        )
        binding.rvDirecciones.layoutManager = LinearLayoutManager(this)
        binding.rvDirecciones.adapter = adapter
    }

    private fun cargarDirecciones() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@DireccionesActivity)
                val response = api.getMisDirecciones()
                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!
                    cantidadDirecciones = lista.size
                    binding.tvContadorDirecciones.text = "Direcciones registradas: $cantidadDirecciones / 15"
                    adapter.actualizarLista(lista)
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "No se pudieron cargar las direcciones")
                    Toast.makeText(this@DireccionesActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DireccionesActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarOpcionesDireccion(d: Direccion) {
        val opciones = if (d.predeterminada) {
            arrayOf("Editar dirección", "Eliminar dirección")
        } else {
            arrayOf("Marcar como predeterminada", "Editar dirección", "Eliminar dirección")
        }

        AlertDialog.Builder(this)
            .setTitle(d.etiqueta)
            .setItems(opciones) { _, which ->
                if (d.predeterminada) {
                    when (which) {
                        0 -> mostrarDialogoFormularioDireccion(d)
                        1 -> eliminarDireccion(d)
                    }
                } else {
                    when (which) {
                        0 -> marcarPredeterminada(d)
                        1 -> mostrarDialogoFormularioDireccion(d)
                        2 -> eliminarDireccion(d)
                    }
                }
            }
            .show()
    }

    private fun marcarPredeterminada(d: Direccion) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@DireccionesActivity)
                val res = api.marcarDireccionPredeterminada(d.idDireccion)
                if (res.isSuccessful) {
                    Toast.makeText(this@DireccionesActivity, "'${d.etiqueta}' fijada como principal", Toast.LENGTH_SHORT).show()
                    cargarDirecciones()
                } else {
                    Toast.makeText(this@DireccionesActivity, ApiErrorUtils.extraerMensaje(res), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DireccionesActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoFormularioDireccion(existente: Direccion?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etEtiqueta = EditText(this).apply {
            hint = "Etiqueta (ej: Casa, Oficina, Bodega)"
            setText(existente?.etiqueta ?: "")
        }
        val etDireccion = EditText(this).apply {
            hint = "Dirección completa (mínimo 8 caracteres)"
            setText(existente?.direccion ?: "")
        }
        val etBarrio = EditText(this).apply {
            hint = "Barrio (opcional)"
            setText(existente?.barrio ?: "")
        }
        val etCiudad = EditText(this).apply {
            hint = "Ciudad"
            setText(existente?.ciudad ?: "Cartagena")
        }
        val etTelefono = EditText(this).apply {
            hint = "Teléfono de contacto (7 a 10 dígitos)"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(existente?.telefono ?: "")
        }
        val cbPredeterminada = CheckBox(this).apply {
            text = "Usar como dirección predeterminada"
            isChecked = existente?.predeterminada ?: (cantidadDirecciones == 0)
        }

        layout.addView(etEtiqueta)
        layout.addView(etDireccion)
        layout.addView(etBarrio)
        layout.addView(etCiudad)
        layout.addView(etTelefono)
        layout.addView(cbPredeterminada)

        AlertDialog.Builder(this)
            .setTitle(if (existente == null) "Nueva Dirección de Entrega" else "Editar Dirección")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val etiqueta = etEtiqueta.text.toString().trim()
                val dir = etDireccion.text.toString().trim()
                val tel = etTelefono.text.toString().trim().replace(" ", "").replace("-", "")

                if (etiqueta.isEmpty() || dir.isEmpty()) {
                    Toast.makeText(this, "Etiqueta y dirección son obligatorias", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (dir.length < 8) {
                    Toast.makeText(this, "La dirección debe tener al menos 8 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (tel.isNotEmpty() && !tel.matches(Regex("^(\\+?57)?[0-9]{7,10}$"))) {
                    Toast.makeText(this, "El teléfono debe tener entre 7 y 10 dígitos numéricos", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val req = DireccionRequest(
                    etiqueta = etiqueta,
                    direccion = dir,
                    barrio = etBarrio.text.toString().trim().ifEmpty { null },
                    ciudad = etCiudad.text.toString().trim().ifEmpty { "Cartagena" },
                    telefono = tel.ifEmpty { null },
                    predeterminada = cbPredeterminada.isChecked
                )

                if (existente == null) {
                    guardarNuevaDireccion(req)
                } else {
                    actualizarDireccion(existente.idDireccion, req)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNuevaDireccion(req: DireccionRequest) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@DireccionesActivity)
                val response = api.crearDireccion(req)
                if (response.isSuccessful) {
                    Toast.makeText(this@DireccionesActivity, "Dirección guardada exitosamente", Toast.LENGTH_SHORT).show()
                    cargarDirecciones()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "Error al guardar (${response.code()})")
                    Toast.makeText(this@DireccionesActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DireccionesActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarDireccion(id: Int, req: DireccionRequest) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@DireccionesActivity)
                val response = api.actualizarDireccion(id, req)
                if (response.isSuccessful) {
                    Toast.makeText(this@DireccionesActivity, "Dirección actualizada", Toast.LENGTH_SHORT).show()
                    cargarDirecciones()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "Error al actualizar (${response.code()})")
                    Toast.makeText(this@DireccionesActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DireccionesActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarDireccion(d: Direccion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Dirección")
            .setMessage("¿Deseas eliminar '${d.etiqueta}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = RetrofitClient.getApiService(this@DireccionesActivity)
                        val res = api.eliminarDireccion(d.idDireccion)
                        if (res.isSuccessful) {
                            Toast.makeText(this@DireccionesActivity, "Dirección eliminada", Toast.LENGTH_SHORT).show()
                            cargarDirecciones()
                        } else {
                            Toast.makeText(this@DireccionesActivity, ApiErrorUtils.extraerMensaje(res), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@DireccionesActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
