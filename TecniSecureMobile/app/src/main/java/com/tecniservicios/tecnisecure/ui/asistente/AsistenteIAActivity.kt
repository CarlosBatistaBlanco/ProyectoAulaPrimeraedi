package com.tecniservicios.tecnisecure.ui.asistente

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.tecniservicios.tecnisecure.R
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.DeepSeekAdvisorClient
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.AsistenteHistorialItem
import com.tecniservicios.tecnisecure.data.model.AsistenteMensaje
import com.tecniservicios.tecnisecure.data.model.AsistenteRequest
import com.tecniservicios.tecnisecure.data.model.MensajeVisitanteRequest
import com.tecniservicios.tecnisecure.databinding.ActivityAsistenteIaBinding
import com.tecniservicios.tecnisecure.ui.pedidos.MisPedidosActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsistenteIAActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenteIaBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var sessionManager: SessionManager

    private var tokenConversacion: String? = null
    private var enModoAsesor: Boolean = false
    private var ultimoIdMensajeRecibido: Int = 0
    private var jobSondeoAsesor: Job? = null
    private val historialCharla = mutableListOf<AsistenteHistorialItem>()

    private val preguntasFrecuentes = listOf(
        "¿Qué cámaras tienen disponibles?",
        "Recomiéndame una cámara para exterior",
        "¿Qué alarmas venden?",
        "¿Cuánto cuesta instalar un sistema?",
        "Hablar con un asesor"
    )

    private val sugerenciasModoAsesor = listOf(
        "Ver mis pedidos",
        "¿Qué cámaras tienen disponibles?",
        "¿Qué garantía tienen los equipos?",
        "Horarios de atención"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsistenteIaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        tokenConversacion = sessionManager.getChatToken()
        enModoAsesor = sessionManager.isChatEscalado()

        binding.toolbarAsistente.setNavigationOnClickListener {
            finish()
        }

        binding.btnReiniciarChat.setOnClickListener {
            reiniciarConversacion()
        }

        setupRecyclerView()

        // Precargar catálogo real en segundo plano para respuestas inmediatas
        lifecycleScope.launch {
            DeepSeekAdvisorClient.asegurarContextoTienda(this@AsistenteIAActivity)
        }

        if (!tokenConversacion.isNullOrBlank()) {
            restaurarConversacionServidor(tokenConversacion!!)
        } else {
            mostrarSaludoInicial()
        }

        actualizarEstadoVisual()

        binding.btnEnviarMensaje.setOnClickListener {
            val texto = binding.etMensajeChat.text.toString().trim()
            if (texto.isNotEmpty()) {
                binding.etMensajeChat.text?.clear()
                procesarEntradaUsuario(texto, desdeChip = false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        iniciarSondeoEnVivo()
    }

    override fun onPause() {
        super.onPause()
        jobSondeoAsesor?.cancel()
        jobSondeoAsesor = null
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(mutableListOf())
        binding.rvMensajesChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMensajesChat.adapter = adapter
    }

    private fun mostrarSaludoInicial() {
        adapter.limpiarMensajes()
        adapter.agregarMensaje(
            AsistenteMensaje(
                texto = "¡Hola, ${sessionManager.getUserName()}! Soy tu asesor inteligente de TECNISECURE.\n" +
                        "Pregúntame qué cámaras, alarmas o controles de acceso necesitas, precios reales, garantías o toca 'Hablar con un asesor'.",
                esUsuario = false,
                hora = obtenerHoraActual()
            )
        )
        renderizarSugerencias(preguntasFrecuentes)
    }

    private fun reiniciarConversacion() {
        sessionManager.limpiarChat()
        tokenConversacion = null
        enModoAsesor = false
        ultimoIdMensajeRecibido = 0
        historialCharla.clear()
        actualizarEstadoVisual()
        mostrarSaludoInicial()
        Toast.makeText(this, "Nueva conversación iniciada", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarEstadoVisual() {
        when {
            enModoAsesor -> {
                binding.layoutBannerEstado.setBackgroundColor(Color.parseColor("#D1FAE5"))
                binding.tvEstadoChat.text = "Conectado con asesor humano · Mensajes en vivo."
                binding.etMensajeChat.hint = "Escribe al asesor en línea..."
                renderizarSugerencias(sugerenciasModoAsesor)
            }
            sessionManager.tieneDeepSeekConfigurado() -> {
                binding.layoutBannerEstado.setBackgroundColor(Color.parseColor("#DBEAFE"))
                binding.tvEstadoChat.text = "Asesor IA activo · Conectado al inventario en tiempo real."
                binding.etMensajeChat.hint = "Escribe tu consulta sobre equipos o servicios..."
                renderizarSugerencias(preguntasFrecuentes)
            }
            else -> {
                binding.layoutBannerEstado.setBackgroundColor(Color.parseColor("#E0F2FE"))
                binding.tvEstadoChat.text = "Asesor inteligente en línea · Catálogo y soporte."
                binding.etMensajeChat.hint = "Escribe tu consulta sobre equipos o servicios..."
                renderizarSugerencias(preguntasFrecuentes)
            }
        }
    }

    private fun restaurarConversacionServidor(token: String) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@AsistenteIAActivity)
                val res = api.getMensajesConversacion(token)
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val mensajes = res.body()!!
                    adapter.limpiarMensajes()
                    mensajes.forEach { m ->
                        if (m.idMensaje > ultimoIdMensajeRecibido) {
                            ultimoIdMensajeRecibido = m.idMensaje
                        }
                        val esVisitante = m.remitente.equals("visitante", ignoreCase = true)
                        val prefijo = if (m.remitente.equals("asesor", ignoreCase = true)) {
                            "Asesor TECNISECURE:\n"
                        } else ""
                        val hora = ApiErrorUtils.formatearFecha(m.creadoEn).takeLast(5)
                        adapter.agregarMensaje(
                            AsistenteMensaje(
                                texto = DeepSeekAdvisorClient.limpiarEmojis("$prefijo${m.mensaje}"),
                                esUsuario = esVisitante,
                                hora = hora
                            )
                        )
                    }
                    binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
                } else {
                    mostrarSaludoInicial()
                }
            } catch (_: Exception) {
                mostrarSaludoInicial()
            }
        }
    }

    private fun iniciarSondeoEnVivo() {
        jobSondeoAsesor?.cancel()
        jobSondeoAsesor = lifecycleScope.launch {
            while (isActive) {
                val token = tokenConversacion
                if (!token.isNullOrBlank()) {
                    try {
                        val api = RetrofitClient.getApiService(this@AsistenteIAActivity)
                        val res = api.getMensajesConversacion(token)
                        if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                            val nuevosAsesor = res.body()!!.filter { m ->
                                m.idMensaje > ultimoIdMensajeRecibido &&
                                        m.remitente.equals("asesor", ignoreCase = true)
                            }
                            if (nuevosAsesor.isNotEmpty()) {
                                nuevosAsesor.forEach { m ->
                                    if (m.idMensaje > ultimoIdMensajeRecibido) {
                                        ultimoIdMensajeRecibido = m.idMensaje
                                    }
                                    adapter.agregarMensaje(
                                        AsistenteMensaje(
                                            texto = DeepSeekAdvisorClient.limpiarEmojis("Asesor TECNISECURE:\n${m.mensaje}"),
                                            esUsuario = false,
                                            hora = ApiErrorUtils.formatearFecha(m.creadoEn).takeLast(5)
                                        )
                                    )
                                }
                                enModoAsesor = true
                                sessionManager.setChatEscalado(true)
                                binding.layoutBannerEstado.setBackgroundColor(Color.parseColor("#D1FAE5"))
                                binding.tvEstadoChat.text = "Un asesor te respondió en vivo"
                                binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
                            } else {
                                val maxId = res.body()!!.maxOfOrNull { it.idMensaje } ?: ultimoIdMensajeRecibido
                                if (maxId > ultimoIdMensajeRecibido) {
                                    ultimoIdMensajeRecibido = maxId
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(4000L)
            }
        }
    }

    private fun renderizarSugerencias(sugerencias: List<String>) {
        binding.layoutSugerenciasChat.removeAllViews()
        val densidad = resources.displayMetrics.density
        val filtradas = if (enModoAsesor) {
            sugerencias.filterNot { it.contains("asesor", ignoreCase = true) }
        } else {
            sugerencias
        }

        filtradas.forEach { pregunta ->
            val btn = MaterialButton(this).apply {
                text = pregunta
                textSize = 11f
                isAllCaps = false
                cornerRadius = (16 * densidad).toInt()
                backgroundTintList = ContextCompat.getColorStateList(this@AsistenteIAActivity, R.color.card_bg)
                setTextColor(ContextCompat.getColor(this@AsistenteIAActivity, R.color.cian_primario))
                strokeColor = ContextCompat.getColorStateList(this@AsistenteIAActivity, R.color.cian_primario)
                strokeWidth = (1 * densidad).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    (36 * densidad).toInt()
                ).apply {
                    marginEnd = (8 * densidad).toInt()
                }
                setOnClickListener {
                    procesarEntradaUsuario(pregunta, desdeChip = true)
                }
            }
            binding.layoutSugerenciasChat.addView(btn)
        }
    }

    private fun pideAsesorHumano(texto: String): Boolean {
        val norm = Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return listOf("asesor", "humano", "persona real", "agente", "operador", "que me llamen", "reclamo", "queja")
            .any { norm.contains(it) }
    }

    private fun procesarEntradaUsuario(texto: String, desdeChip: Boolean) {
        if (texto.equals("Ver mis pedidos", ignoreCase = true)) {
            startActivity(Intent(this, MisPedidosActivity::class.java))
            return
        }

        if (enModoAsesor && !desdeChip && !tokenConversacion.isNullOrBlank()) {
            enviarDirectoAlAsesor(texto)
            return
        }

        responderComoAsesorInteligente(texto)
    }

    private fun enviarDirectoAlAsesor(texto: String) {
        adapter.agregarMensaje(
            AsistenteMensaje(
                texto = texto,
                esUsuario = true,
                hora = obtenerHoraActual()
            )
        )
        binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
        binding.btnEnviarMensaje.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@AsistenteIAActivity)
                val req = MensajeVisitanteRequest(
                    mensaje = texto,
                    token = tokenConversacion,
                    nombre = sessionManager.getUserName().takeIf { it.isNotBlank() },
                    correo = sessionManager.getUserEmail().takeIf { it.isNotBlank() },
                    telefono = sessionManager.getUserPhone().takeIf { it.isNotBlank() }
                )
                val res = api.enviarMensajeAsesor(req)
                if (res.isSuccessful && res.body() != null) {
                    val nuevoToken = res.body()!!.token
                    if (!nuevoToken.isNullOrBlank()) {
                        tokenConversacion = nuevoToken
                        sessionManager.setChatToken(nuevoToken)
                    }
                    binding.tvEstadoChat.text = "Mensaje enviado al asesor en vivo"
                } else {
                    Toast.makeText(this@AsistenteIAActivity, "No se pudo entregar el mensaje (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AsistenteIAActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnEnviarMensaje.isEnabled = true
                binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun responderComoAsesorInteligente(texto: String) {
        adapter.agregarMensaje(
            AsistenteMensaje(
                texto = texto,
                esUsuario = true,
                hora = obtenerHoraActual()
            )
        )
        binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
        binding.btnEnviarMensaje.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@AsistenteIAActivity)

                // 1) Si el usuario pidió explícitamente hablar con un asesor humano, escalar en el backend
                if (pideAsesorHumano(texto)) {
                    val response = api.conversarAsistente(
                        AsistenteRequest(
                            mensaje = texto,
                            token = tokenConversacion,
                            historial = historialCharla.takeLast(8),
                            nombre = sessionManager.getUserName().takeIf { it.isNotBlank() },
                            correo = sessionManager.getUserEmail().takeIf { it.isNotBlank() },
                            telefono = sessionManager.getUserPhone().takeIf { it.isNotBlank() }
                        )
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        res.getTokenVal()?.let {
                            tokenConversacion = it
                            sessionManager.setChatToken(it)
                        }
                        val respuestaTexto = DeepSeekAdvisorClient.limpiarEmojis(
                            (res.respuesta ?: "Solicitud enviada al equipo de soporte.").trim()
                        )
                        adapter.agregarMensaje(AsistenteMensaje(respuestaTexto, false, obtenerHoraActual()))
                        enModoAsesor = true
                        sessionManager.setChatEscalado(true)
                        actualizarEstadoVisual()
                        sincronizarUltimoIdHilo()
                    }
                    return@launch
                }

                // 2) Consultar DeepSeek LLM con la API Key configurada internamente en local.properties
                val internalKey = sessionManager.getDeepSeekApiKey()
                if (internalKey.isNotBlank()) {
                    val iaDirecta = DeepSeekAdvisorClient.consultarDeepSeekDirecto(
                        context = this@AsistenteIAActivity,
                        apiKey = internalKey,
                        preguntaActual = texto,
                        historial = historialCharla
                    )
                    if (!iaDirecta.isNullOrBlank()) {
                        historialCharla.add(AsistenteHistorialItem("user", texto))
                        historialCharla.add(AsistenteHistorialItem("assistant", iaDirecta))
                        adapter.agregarMensaje(AsistenteMensaje(iaDirecta, false, obtenerHoraActual()))
                        return@launch
                    }
                }

                // 3) Respaldo inteligente alimentado con el catálogo real en vivo de Render
                val respuestaCatalogo = DeepSeekAdvisorClient.responderConCatalogoEnVivo(this@AsistenteIAActivity, texto)
                if (!respuestaCatalogo.isNullOrBlank()) {
                    historialCharla.add(AsistenteHistorialItem("user", texto))
                    historialCharla.add(AsistenteHistorialItem("assistant", respuestaCatalogo))
                    adapter.agregarMensaje(AsistenteMensaje(respuestaCatalogo, false, obtenerHoraActual()))
                    return@launch
                }

                // 4) Respaldo secundario del servidor
                val response = api.conversarAsistente(
                    AsistenteRequest(
                        mensaje = texto,
                        token = tokenConversacion,
                        historial = historialCharla.takeLast(8),
                        nombre = sessionManager.getUserName().takeIf { it.isNotBlank() },
                        correo = sessionManager.getUserEmail().takeIf { it.isNotBlank() },
                        telefono = sessionManager.getUserPhone().takeIf { it.isNotBlank() }
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    res.getTokenVal()?.let {
                        tokenConversacion = it
                        sessionManager.setChatToken(it)
                    }
                    val respuestaBot = DeepSeekAdvisorClient.limpiarEmojis(
                        (res.respuesta ?: "¿Podrías indicarme qué tipo de espacio deseas asegurar (casa, oficina o exterior)?").trim()
                    )
                    historialCharla.add(AsistenteHistorialItem("user", texto))
                    historialCharla.add(AsistenteHistorialItem("assistant", respuestaBot))
                    adapter.agregarMensaje(AsistenteMensaje(respuestaBot, false, obtenerHoraActual()))
                    if (res.escalado == true) {
                        enModoAsesor = true
                        sessionManager.setChatEscalado(true)
                        actualizarEstadoVisual()
                    }
                    sincronizarUltimoIdHilo()
                }
            } catch (e: Exception) {
                adapter.agregarMensaje(
                    AsistenteMensaje(
                        texto = "Sin conexión temporal (${e.localizedMessage}). Intenta de nuevo.",
                        esUsuario = false,
                        hora = obtenerHoraActual()
                    )
                )
            } finally {
                binding.btnEnviarMensaje.isEnabled = true
                binding.rvMensajesChat.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private suspend fun sincronizarUltimoIdHilo() {
        val tok = tokenConversacion ?: return
        try {
            val hiloRes = RetrofitClient.getApiService(this).getMensajesConversacion(tok)
            if (hiloRes.isSuccessful) {
                ultimoIdMensajeRecibido = hiloRes.body()?.maxOfOrNull { it.idMensaje } ?: ultimoIdMensajeRecibido
            }
        } catch (_: Exception) {
        }
    }

    private fun obtenerHoraActual(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
