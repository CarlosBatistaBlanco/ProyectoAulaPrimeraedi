package com.tecniservicios.tecnisecure.ui.tareas

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.AsignarTareaRequest
import com.tecniservicios.tecnisecure.data.model.CalificarTareaRequest
import com.tecniservicios.tecnisecure.data.model.ProgresoTareaRequest
import com.tecniservicios.tecnisecure.data.model.TareaEmpleado
import com.tecniservicios.tecnisecure.databinding.ActivityTareasEmpleadoBinding
import kotlinx.coroutines.launch

class TareasEmpleadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTareasEmpleadoBinding
    private lateinit var adapter: TareaAdapter
    private lateinit var sessionManager: SessionManager
    private var esAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTareasEmpleadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        esAdmin = sessionManager.esAdmin()

        if (esAdmin) {
            binding.toolbarTareas.title = "Supervisión Tareas v2.0"
            binding.layoutBarraAdminTareas.visibility = View.VISIBLE
            binding.btnAsignarTareaAdmin.setOnClickListener {
                abrirDialogoAsignarTarea()
            }
        } else {
            binding.toolbarTareas.title = "Mis Tareas Operativas v2.0"
            binding.layoutBarraAdminTareas.visibility = View.GONE
        }

        binding.toolbarTareas.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        cargarTareas()

        binding.swipeTareas.setOnRefreshListener {
            cargarTareas()
        }
    }

    private fun setupRecyclerView() {
        adapter = TareaAdapter(
            tareas = emptyList(),
            esModoAdmin = esAdmin,
            onAccionPrincipalClick = { tarea ->
                if (esAdmin) {
                    mostrarDialogoRevisionAdmin(tarea)
                } else {
                    mostrarDialogoEntregaEmpleado(tarea)
                }
            },
            onHistorialClick = { tarea ->
                consultarHistorial(tarea)
            }
        )
        binding.rvTareas.layoutManager = LinearLayoutManager(this)
        binding.rvTareas.adapter = adapter
    }

    private fun cargarTareas() {
        binding.swipeTareas.isRefreshing = true
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val response = if (esAdmin) {
                    api.getTodasLasTareas()
                } else {
                    api.getMisTareas()
                }

                if (response.isSuccessful && response.body() != null) {
                    val tareas = response.body()!!
                    adapter.actualizarLista(tareas)
                    binding.tvTareasVacio.visibility = if (tareas.isEmpty()) View.VISIBLE else View.GONE

                    if (esAdmin) {
                        val enRevision = tareas.count { it.estado.equals("completada", ignoreCase = true) }
                        binding.tvResumenTareas.text = "Total: ${tareas.size} · Por revisar: $enRevision"
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    Toast.makeText(this@TareasEmpleadoActivity, "Acceso exclusivo para personal autorizado", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "No se pudieron obtener tareas (${response.code()})")
                    Toast.makeText(this@TareasEmpleadoActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeTareas.isRefreshing = false
            }
        }
    }

    private fun mostrarDialogoEntregaEmpleado(tarea: TareaEmpleado) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val etEvidencia = EditText(this).apply {
            hint = "Evidencia (ej: acta_instalacion_camaras.jpg)"
            setText(tarea.evidencia ?: "")
        }
        val etComentario = EditText(this).apply {
            hint = "Detalle técnico del trabajo ejecutado"
            setText(tarea.comentarioEmpleado ?: "")
        }

        layout.addView(etEvidencia)
        layout.addView(etComentario)

        AlertDialog.Builder(this)
            .setTitle("Entregar Tarea v2.0")
            .setMessage("Al marcar como 'completada', la tarea quedará bloqueada en modo solo lectura mientras el administrador revisa y califica la entrega.")
            .setView(layout)
            .setPositiveButton("Entregar y Bloquear") { _, _ ->
                val evidencia = etEvidencia.text.toString().trim()
                val comentario = etComentario.text.toString().trim()
                enviarProgresoEmpleado(
                    idTarea = tarea.idTarea,
                    nuevoEstado = "completada",
                    evidencia = evidencia.ifEmpty { "evidencia_campo_${tarea.idTarea}.jpg" },
                    comentario = comentario.ifEmpty { "Servicio técnico ejecutado y verificado en sitio." }
                )
            }
            .setNeutralButton("Guardar Avance") { _, _ ->
                val evidencia = etEvidencia.text.toString().trim()
                val comentario = etComentario.text.toString().trim()
                enviarProgresoEmpleado(
                    idTarea = tarea.idTarea,
                    nuevoEstado = "en_progreso",
                    evidencia = evidencia.ifEmpty { null },
                    comentario = comentario.ifEmpty { "Tarea en ejecución por el técnico." }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarProgresoEmpleado(idTarea: Int, nuevoEstado: String, evidencia: String?, comentario: String?) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val request = ProgresoTareaRequest(
                    estado = nuevoEstado,
                    evidencia = evidencia,
                    comentarioEmpleado = comentario
                )
                val response = api.actualizarProgresoTarea(idTarea, request)

                if (response.isSuccessful) {
                    val aviso = if (nuevoEstado == "completada") {
                        "¡Tarea entregada y bloqueada para revisión!"
                    } else {
                        "Avance guardado en progreso"
                    }
                    Toast.makeText(this@TareasEmpleadoActivity, aviso, Toast.LENGTH_LONG).show()
                    cargarTareas()
                } else {
                    val msg = ApiErrorUtils.extraerMensaje(response, "Error al actualizar tarea (${response.code()})")
                    Toast.makeText(this@TareasEmpleadoActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Falla: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoRevisionAdmin(tarea: TareaEmpleado) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val calificaciones = listOf(
            "5/5 — Excelente ejecución",
            "4/5 — Muy buen desempeño",
            "3/5 — Cumple lo solicitado",
            "2/5 — Requiere mejoras",
            "1/5 — Deficiente"
        )
        val spinnerNota = Spinner(this).apply {
            adapter = ArrayAdapter(this@TareasEmpleadoActivity, android.R.layout.simple_spinner_dropdown_item, calificaciones)
        }

        val etObservacion = EditText(this).apply {
            hint = "Observación administrativa o corrección solicitada"
            setText(tarea.observacionAdmin ?: "")
        }

        layout.addView(spinnerNota)
        layout.addView(etObservacion)

        val detalleEntrega = buildString {
            appendLine("Responsable: ${tarea.empleadoNombre ?: "Empleado #${tarea.idEmpleadoUsuario ?: ""}"}")
            appendLine("Estado actual: ${tarea.estado.uppercase()}")
            if (!tarea.evidencia.isNullOrBlank()) appendLine("Evidencia: ${tarea.evidencia}")
            if (!tarea.comentarioEmpleado.isNullOrBlank()) appendLine("Reporte técnico: ${tarea.comentarioEmpleado}")
        }

        AlertDialog.Builder(this)
            .setTitle("Revisión Administrativa — #${tarea.idTarea}")
            .setMessage(detalleEntrega)
            .setView(layout)
            .setPositiveButton("Aprobar y Calificar") { _, _ ->
                val estrellas = 5 - spinnerNota.selectedItemPosition
                val obs = etObservacion.text.toString().trim().ifEmpty { "Entrega verificada y aprobada." }
                enviarCalificacionAdmin(tarea.idTarea, "revisada", estrellas, obs)
            }
            .setNeutralButton("Devolver Tarea") { _, _ ->
                val obs = etObservacion.text.toString().trim()
                    .ifEmpty { "Por favor adjuntar evidencia adicional y detallar pruebas realizadas." }
                enviarCalificacionAdmin(tarea.idTarea, "en_progreso", null, obs)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarCalificacionAdmin(idTarea: Int, estadoDestino: String, calificacion: Int?, obs: String?) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val res = api.calificarTarea(
                    idTarea,
                    CalificarTareaRequest(
                        estado = estadoDestino,
                        calificacion = calificacion,
                        observacionAdmin = obs
                    )
                )
                if (res.isSuccessful) {
                    val msg = if (estadoDestino == "revisada") {
                        "Tarea aprobada con $calificacion/5 estrellas"
                    } else {
                        "Tarea devuelta al empleado para corrección (desbloqueada)"
                    }
                    Toast.makeText(this@TareasEmpleadoActivity, msg, Toast.LENGTH_LONG).show()
                    cargarTareas()
                } else {
                    Toast.makeText(this@TareasEmpleadoActivity, ApiErrorUtils.extraerMensaje(res), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirDialogoAsignarTarea() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val res = api.getEmpleados()
                val empleados = if (res.isSuccessful) res.body().orEmpty() else emptyList()
                if (empleados.isEmpty()) {
                    Toast.makeText(this@TareasEmpleadoActivity, "No hay empleados activos para asignar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val layout = LinearLayout(this@TareasEmpleadoActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 30, 50, 10)
                }

                val spinnerEmpleados = Spinner(this@TareasEmpleadoActivity).apply {
                    adapter = ArrayAdapter(
                        this@TareasEmpleadoActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        empleados.map { "${it.nombre} (${it.area ?: "OPERATIVO"})" }
                    )
                }

                val etTitulo = EditText(this@TareasEmpleadoActivity).apply {
                    hint = "Título de la tarea operativa"
                }
                val etDescripcion = EditText(this@TareasEmpleadoActivity).apply {
                    hint = "Instrucciones técnicas"
                }
                val prioridades = listOf("alta", "media", "baja")
                val spinnerPrioridad = Spinner(this@TareasEmpleadoActivity).apply {
                    adapter = ArrayAdapter(
                        this@TareasEmpleadoActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        prioridades.map { "Prioridad: ${it.uppercase()}" }
                    )
                }

                layout.addView(spinnerEmpleados)
                layout.addView(etTitulo)
                layout.addView(etDescripcion)
                layout.addView(spinnerPrioridad)

                AlertDialog.Builder(this@TareasEmpleadoActivity)
                    .setTitle("Asignar Nueva Tarea")
                    .setView(layout)
                    .setPositiveButton("Asignar") { _, _ ->
                        val titulo = etTitulo.text.toString().trim()
                        if (titulo.isEmpty()) {
                            Toast.makeText(this@TareasEmpleadoActivity, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val emp = empleados[spinnerEmpleados.selectedItemPosition]
                        val prio = prioridades[spinnerPrioridad.selectedItemPosition]
                        val desc = etDescripcion.text.toString().trim().ifEmpty { null }
                        crearTareaAdmin(emp.idUsuario, titulo, desc, prio)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearTareaAdmin(idEmpleado: Int, titulo: String, descripcion: String?, prioridad: String) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val res = api.asignarTarea(
                    AsignarTareaRequest(
                        idEmpleadoUsuario = idEmpleado,
                        titulo = titulo,
                        descripcion = descripcion,
                        prioridad = prioridad
                    )
                )
                if (res.isSuccessful) {
                    Toast.makeText(this@TareasEmpleadoActivity, "Tarea asignada exitosamente", Toast.LENGTH_SHORT).show()
                    cargarTareas()
                } else {
                    Toast.makeText(this@TareasEmpleadoActivity, ApiErrorUtils.extraerMensaje(res), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun consultarHistorial(tarea: TareaEmpleado) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@TareasEmpleadoActivity)
                val response = api.getHistorialTarea(tarea.idTarea)

                if (response.isSuccessful && response.body() != null) {
                    val historial = response.body()!!
                    val mensaje = if (historial.isEmpty()) {
                        "Aún no hay entregas registradas en la bitácora inmutable para esta tarea."
                    } else {
                        historial.joinToString("\n\n────────────────────\n\n") { h ->
                            buildString {
                                appendLine("Intento #${h.numeroIntento} · ${h.estadoRevision.replace("_", " ").uppercase()}")
                                appendLine("Enviado: ${ApiErrorUtils.formatearFecha(h.enviadoEn)}")
                                appendLine("Evidencia: ${h.evidencia ?: "Sin adjunto"}")
                                if (!h.comentarioEmpleado.isNullOrBlank()) {
                                    appendLine("Nota técnico: ${h.comentarioEmpleado}")
                                }
                                if (h.calificacion != null) {
                                    appendLine("Calificación: ${h.calificacion}/5")
                                }
                                append("Observación Admin: ${h.observacionAdmin ?: "En espera de revisión"}")
                            }
                        }
                    }

                    AlertDialog.Builder(this@TareasEmpleadoActivity)
                        .setTitle("Bitácora Inmutable — Tarea #${tarea.idTarea}")
                        .setMessage(mensaje)
                        .setPositiveButton("Cerrar", null)
                        .show()
                } else {
                    Toast.makeText(this@TareasEmpleadoActivity, ApiErrorUtils.extraerMensaje(response, "No se pudo consultar el historial"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TareasEmpleadoActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
