package com.tecniservicios.tecnisecure.data.api

import org.json.JSONObject
import retrofit2.Response

object ApiErrorUtils {

    fun extraerMensaje(response: Response<*>, porDefecto: String = "Error (${response.code()})"): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (raw.isNullOrBlank()) return porDefecto

        return try {
            val json = JSONObject(raw)
            val campos = json.optJSONObject("campos")
            if (campos != null && campos.length() > 0) {
                val detalles = mutableListOf<String>()
                val keys = campos.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val msg = campos.optString(key)
                    if (msg.isNotBlank()) detalles.add(msg)
                }
                if (detalles.isNotEmpty()) return detalles.joinToString("\n")
            }
            val mensaje = json.optString("mensaje").ifBlank { json.optString("message") }
            if (mensaje.isNotBlank()) mensaje else porDefecto
        } catch (_: Exception) {
            porDefecto
        }
    }

    fun formatearFecha(iso: String?): String {
        if (iso.isNullOrBlank()) return "Reciente"
        // Convierte "2026-11-25T22:15:30.123" -> "25/11/2026 22:15"
        val limpio = iso.trim()
        val partes = limpio.split("T", " ")
        val fechaRaw = partes.getOrNull(0) ?: return limpio
        val segmentos = fechaRaw.split("-")
        val fechaFormateada = if (segmentos.size == 3) {
            "${segmentos[2]}/${segmentos[1]}/${segmentos[0]}"
        } else {
            fechaRaw
        }
        val horaRaw = partes.getOrNull(1)?.take(5)
        return if (!horaRaw.isNullOrBlank()) "$fechaFormateada $horaRaw" else fechaFormateada
    }
}
