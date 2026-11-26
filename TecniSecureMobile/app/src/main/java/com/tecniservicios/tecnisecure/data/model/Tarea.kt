package com.tecniservicios.tecnisecure.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TareaEmpleado(
    @SerializedName("idTarea") val idTarea: Int = 0,
    @SerializedName("idEmpleadoUsuario") val idEmpleadoUsuario: Int? = null,
    @SerializedName("empleadoNombre") val empleadoNombre: String? = null,
    @SerializedName("titulo") val titulo: String = "",
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("prioridad") val prioridad: String = "media",
    @SerializedName("estado") val estado: String = "pendiente", // "pendiente", "en_progreso", "completada", "revisada"
    @SerializedName("evidencia") val evidencia: String? = null,
    @SerializedName("comentarioEmpleado") val comentarioEmpleado: String? = null,
    @SerializedName("calificacion") val calificacion: Int? = null,
    @SerializedName("observacionAdmin") val observacionAdmin: String? = null,
    @SerializedName("fechaLimite") val fechaLimite: String? = null,
    @SerializedName("totalEntregas") val totalEntregas: Int? = null
) : Serializable

data class ProgresoTareaRequest(
    @SerializedName("estado") val estado: String,
    @SerializedName("evidencia") val evidencia: String? = null,
    @SerializedName("comentarioEmpleado") val comentarioEmpleado: String? = null
)

data class CalificarTareaRequest(
    @SerializedName("estado") val estado: String, // "revisada" o "en_progreso"
    @SerializedName("calificacion") val calificacion: Int? = null,
    @SerializedName("observacionAdmin") val observacionAdmin: String? = null
)

data class AsignarTareaRequest(
    @SerializedName("idEmpleadoUsuario") val idEmpleadoUsuario: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("prioridad") val prioridad: String = "media",
    @SerializedName("fechaLimite") val fechaLimite: String? = null
)

data class EmpleadoResumen(
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("area") val area: String? = null,
    @SerializedName("cargaActiva") val cargaActiva: Int? = 0
)

data class TareaHistorialEntrega(
    @SerializedName("idEntrega") val idEntrega: Int = 0,
    @SerializedName("numeroIntento") val numeroIntento: Int = 1,
    @SerializedName("evidencia") val evidencia: String? = null,
    @SerializedName("comentarioEmpleado") val comentarioEmpleado: String? = null,
    @SerializedName("estadoRevision") val estadoRevision: String = "en_revision", // "en_revision", "aprobada", "devuelta"
    @SerializedName("calificacion") val calificacion: Int? = null,
    @SerializedName("observacionAdmin") val observacionAdmin: String? = null,
    @SerializedName("enviadoEn") val enviadoEn: String? = null,
    @SerializedName("revisadoEn") val revisadoEn: String? = null
) : Serializable

data class AsistenteMensaje(
    val texto: String,
    val esUsuario: Boolean,
    val hora: String
)

data class AsistenteHistorialItem(
    @SerializedName("rol") val rol: String,
    @SerializedName("texto") val texto: String
)

data class AsistenteRequest(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("token") val token: String? = null,
    @SerializedName("historial") val historial: List<AsistenteHistorialItem>? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null
)

data class AsistenteResponse(
    @SerializedName("respuesta") val respuesta: String? = null,
    @SerializedName("fuente") val fuente: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("tokenConversacion") val tokenConversacion: String? = null,
    @SerializedName("escalado") val escalado: Boolean? = false,
    @SerializedName("idSolicitud") val idSolicitud: Int? = null,
    @SerializedName("mensajeEscalado") val mensajeEscalado: String? = null,
    @SerializedName("sugerencias") val sugerencias: List<String>? = null
) {
    fun getTokenVal(): String? = token ?: tokenConversacion
}

data class MensajeVisitanteRequest(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("token") val token: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null
)

data class ConversacionVisitanteResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("idConversacion") val idConversacion: Int? = null,
    @SerializedName("idSolicitud") val idSolicitud: Int? = null,
    @SerializedName("estado") val estado: String? = null
)

data class MensajeVisitanteResponse(
    @SerializedName("idMensaje") val idMensaje: Int = 0,
    @SerializedName("idConversacion") val idConversacion: Int? = null,
    @SerializedName("remitente") val remitente: String = "bot",
    @SerializedName("mensaje") val mensaje: String = "",
    @SerializedName("creadoEn") val creadoEn: String? = null
)
