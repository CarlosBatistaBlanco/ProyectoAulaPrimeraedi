package com.tecniservicios.tecnisecure.data.api

import android.content.Context
import com.tecniservicios.tecnisecure.data.model.AsistenteHistorialItem
import com.tecniservicios.tecnisecure.data.model.Producto
import com.tecniservicios.tecnisecure.data.model.ServicioTecnico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object DeepSeekAdvisorClient {

    private var catalogoCache: List<Producto> = emptyList()
    private var serviciosCache: List<ServicioTecnico> = emptyList()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val palabrasIgnoradas = setOf(
        "hola", "buenas", "como", "esta", "estas", "que", "cual", "cuales", "cuanto", "cuantos",
        "vale", "valen", "cuesta", "cuestan", "precio", "precios", "tienen", "tiene", "hay",
        "disponible", "disponibles", "venden", "vende", "para", "una", "unas", "uno", "unos",
        "los", "las", "del", "con", "sin", "por", "favor", "quiero", "necesito", "busco",
        "recomienda", "recomiendas", "recomendacion", "mejor", "mejores", "equipo", "equipos",
        "producto", "productos", "catalogo", "tienda", "sistema", "sistemas", "me", "puedes", "dar"
    )

    suspend fun asegurarContextoTienda(context: Context) {
        val api = RetrofitClient.getApiService(context)
        if (catalogoCache.isEmpty()) {
            try {
                val res = api.getProductos(pagina = 0, tamanio = 100)
                if (res.isSuccessful) {
                    catalogoCache = res.body()?.items.orEmpty()
                }
            } catch (_: Exception) {
            }
        }
        if (serviciosCache.isEmpty()) {
            try {
                val res = api.getServicios()
                if (res.isSuccessful) {
                    serviciosCache = res.body().orEmpty()
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun consultarDeepSeekDirecto(
        context: Context,
        apiKey: String,
        preguntaActual: String,
        historial: List<AsistenteHistorialItem>
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        asegurarContextoTienda(context)

        try {
            val systemPrompt = construirPromptSistema()
            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().put("role", "system").put("content", systemPrompt))

            historial.takeLast(8).forEach { item ->
                val rol = if (item.rol == "assistant" || item.rol == "asistente") "assistant" else "user"
                messagesArray.put(JSONObject().put("role", rol).put("content", item.texto))
            }
            messagesArray.put(JSONObject().put("role", "user").put("content", preguntaActual))

            val payload = JSONObject()
                .put("model", "deepseek-chat")
                .put("stream", false)
                .put("temperature", 0.4)
                .put("max_tokens", 550)
                .put("messages", messagesArray)

            val req = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val res = httpClient.newCall(req).execute()
            val raw = res.body?.string()
            if (!res.isSuccessful || raw.isNullOrBlank()) {
                return@withContext null
            }

            val json = JSONObject(raw)
            val choices = json.optJSONArray("choices") ?: return@withContext null
            if (choices.length() == 0) return@withContext null
            val content = choices.getJSONObject(0)
                .optJSONObject("message")
                ?.optString("content")
                ?.let { limpiarEmojis(it) }
                ?.trim()

            content?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Asesor experto local basado en el inventario real en vivo de Render.
     * Entiende preguntas abiertas, plurales, sin tildes, comparaciones y recomendaciones por uso.
     */
    suspend fun responderConCatalogoEnVivo(context: Context, preguntaRaw: String): String? {
        asegurarContextoTienda(context)
        val texto = normalizar(preguntaRaw)
        if (texto.isEmpty()) return null

        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }

        if (contiene(texto, "gracias", "adios", "chao", "perfecto", "excelente")) {
            return "¡Con mucho gusto! Si deseas agregar algún equipo al carrito o necesitas que un técnico revise tu instalación, aquí sigo disponible."
        }
        if (contiene(texto, "horario", "hora de atencion", "abierto", "domingo", "ubicacion", "donde estan")) {
            return "Atendemos de lunes a sábado de 8:00 a.m. a 6:00 p.m. en Cartagena, con despachos a nivel nacional.\nCorreo de soporte: soporte@tecnisecure.com"
        }
        if (contiene(texto, "garantia", "garantias", "factura", "dian", "cufe")) {
            return "Todos nuestros equipos incluyen garantía mínima de 12 meses contra defectos de fábrica y factura electrónica oficial DIAN con código CUFE verificable."
        }
        if (contiene(texto, "pago", "pagar", "tarjeta", "pse", "transferencia", "contraentrega", "efectivo")) {
            return "Puedes pagar por PSE / transferencia bancaria, tarjeta de crédito o débito, o pago contraentrega. Tu factura electrónica se genera al instante."
        }
        if (contiene(texto, "envio", "domicilio", "entrega", "demora")) {
            return "Realizamos entregas rápidas en Cartagena y envíos nacionales asegurados entre 24 y 48 horas hábiles."
        }

        val pideServicio = contiene(texto, "instalar", "instalacion", "mantenimiento", "cita", "visita", "servicio")
        val buscaCamara = contiene(texto, "camara", "cctv", "domo", "bullet", "ptz", "vigilancia", "4k")
        val buscaAlarma = contiene(texto, "alarma", "sirena", "sensor", "humo", "movimiento", "intrusion")
        val buscaAcceso = contiene(texto, "acceso", "biometric", "huella", "cerradura", "rfid", "videoportero", "puerta")
        val buscaAccesorio = contiene(texto, "accesorio", "balun", "cable", "utp", "disco", "ssd", "ups", "fuente", "bateria")

        if (pideServicio && !buscaCamara && !buscaAlarma && !buscaAcceso && !buscaAccesorio) {
            if (serviciosCache.isNotEmpty()) {
                return buildString {
                    appendLine("Ofrecemos estos servicios técnicos certificados:")
                    serviciosCache.take(4).forEach { s ->
                        appendLine("• ${s.titulo}: ${s.descripcion}")
                    }
                    append("El valor final depende de los puntos a instalar. Si quieres agendar visita técnica, toca 'Hablar con un asesor'.")
                }
            }
        }

        if (catalogoCache.isEmpty()) return null

        val raices = texto.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in palabrasIgnoradas }
            .map { raizSingular(it) }
            .distinct()

        val puntuados = catalogoCache.map { p ->
            var pts = 0
            val nom = normalizar(p.nombre)
            val cat = normalizar(p.categoria)
            val mar = normalizar(p.marca)
            val desc = normalizar(p.descripcion)

            if (buscaCamara && !buscaAccesorio) {
                if (cat.contains("camara")) pts += 15
                if (cat.contains("accesorio")) pts -= 10
            }
            if (buscaAlarma && cat.contains("alarma")) pts += 15
            if (buscaAcceso && cat.contains("acceso")) pts += 15
            if (buscaAccesorio && cat.contains("accesorio")) pts += 15

            for (r in raices) {
                if (nom.contains(r)) pts += 9
                if (mar.contains(r)) pts += 7
                if (desc.contains(r)) pts += 4
                if (cat.contains(r)) pts += 3
            }

            if (contiene(texto, "exterior", "calle", "intemperie", "patio", "lluvia") &&
                contiene("$nom $desc", "ip66", "ip67", "exterior", "bullet", "solar", "ptz")
            ) {
                pts += 12
            }
            if (contiene(texto, "interior", "casa", "apartamento", "oficina", "bebe", "sala") &&
                contiene("$nom $desc", "interior", "wifi", "domo")
            ) {
                pts += 10
            }
            if (contiene(texto, "barat", "econom", "menor precio")) {
                pts += ((1_000_000 - p.getPrecioFinal().coerceAtMost(1_000_000.0)) / 150_000).toInt()
            }
            if (p.destacado == true) pts += 1

            p to pts
        }.filter { it.second > 2 }
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }

        if (puntuados.isNotEmpty()) {
            return buildString {
                appendLine("Según tu consulta, estos son los equipos recomendados en stock:")
                puntuados.forEach { p ->
                    val descTxt = if ((p.descuentoPct ?: 0.0) > 0) " (-${p.descuentoPct!!.toInt()}%)" else ""
                    val resumen = p.descripcion?.let { d ->
                        val corte = if (d.length > 70) d.take(68) + "..." else d
                        "\n  ↳ $corte"
                    } ?: ""
                    appendLine("• ${p.nombre} (${p.marca ?: "Oficial"}): ${formatoPesos.format(p.getPrecioFinal())}$descTxt · Stock: ${p.stock ?: 0}$resumen")
                }
                append("¿Te gustaría comparar alguno en detalle o que te recomiende el kit completo?")
            }
        }

        if (contiene(texto, "catalogo", "productos", "venden", "tienen", "categorias", "que ofrecen")) {
            return "Nuestro catálogo cuenta con 28 equipos en 4 líneas:\n" +
                    "• Cámaras de Seguridad (Domo 4K, Bullet IP, PTZ 360°, WiFi 2K, Solar)\n" +
                    "• Alarmas (Kits inalámbricos, sensores PIR, humo, sirenas)\n" +
                    "• Control de Acceso (Biométricos de huella/rostro, cerraduras magnéticas, RFID)\n" +
                    "• Accesorios (Discos vigilancia, UPS, baluns, cable UTP, fuentes)\n" +
                    "Dime qué espacio deseas proteger (casa, local, bodega o exterior) y te armo la recomendación exacta."
        }

        return null
    }

    private fun construirPromptSistema(): String {
        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        return buildString {
            appendLine("Eres el Ingeniero Asesor de Seguridad Electrónica de TECNISECURE (Cartagena, Colombia).")
            appendLine("Atiendes clientes de manera independiente, cálida, experta y directa en español.")
            appendLine("Reglas estrictas:")
            appendLine("1. Basa tus recomendaciones y precios EXCLUSIVAMENTE en el inventario real en vivo listado abajo.")
            appendLine("2. Si el cliente pregunta por cámaras, alarmas, control de acceso o accesorios, recomienda las referencias exactas con su marca, precio final en pesos colombianos, descuento (si aplica), stock y por qué le sirven.")
            appendLine("3. Garantía mínima: 12 meses. Factura electrónica DIAN con IVA 19% incluido. Pagos: PSE/transferencia, tarjeta o contraentrega. Envíos: 24-48h hábiles. Horario: Lun-Sáb 8:00 a.m. a 6:00 p.m.")
            appendLine("4. Sé claro y conciso (máximo 140 palabras), usando viñetas limpias con guiones (-). NO uses emojis bajo ninguna circunstancia.")
            appendLine()
            appendLine("INVENTARIO EN TIEMPO REAL DE TECNISECURE:")
            catalogoCache.forEach { p ->
                val rebaja = if ((p.descuentoPct ?: 0.0) > 0) " [Oferta -${p.descuentoPct!!.toInt()}%]" else ""
                appendLine("- [${p.categoria ?: "Seguridad"}] ${p.nombre} (${p.marca ?: "Genérico"}): ${formatoPesos.format(p.getPrecioFinal())}$rebaja | Stock: ${p.stock ?: 0} | ${p.descripcion ?: ""}")
            }
            if (serviciosCache.isNotEmpty()) {
                appendLine()
                appendLine("SERVICIOS TÉCNICOS:")
                serviciosCache.forEach { s ->
                    appendLine("- ${s.titulo}: ${s.descripcion}")
                }
            }
        }
    }

    fun limpiarEmojis(texto: String): String {
        return texto.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F]"), "")
            .replace(Regex(" +"), " ")
    }

    private fun contiene(texto: String, vararg claves: String): Boolean {
        return claves.any { texto.contains(it) }
    }

    private fun normalizar(s: String?): String {
        if (s.isNullOrBlank()) return ""
        val sinTildes = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return sinTildes.replace(Regex("\\s+"), " ").trim()
    }

    private fun raizSingular(palabra: String): String {
        if (palabra.length > 5 && palabra.endsWith("ores")) return palabra.dropLast(2)
        if (palabra.length > 4 && palabra.endsWith("s") && !palabra.endsWith("ups")) return palabra.dropLast(1)
        return palabra
    }
}
