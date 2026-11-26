package com.tecniservicios.tecnisecure.data.model

import com.google.gson.annotations.SerializedName

data class CaptchaResponse(
    @SerializedName("pregunta") val pregunta: String,
    @SerializedName("token") val token: String
)

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("captcha") val captcha: String,
    @SerializedName("captchaToken") val captchaToken: String
)

data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("documento") val documento: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("direccion") val direccion: String? = null
)

data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("usuario") val usuario: UsuarioResponse
)

data class UsuarioResponse(
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("avatar") val avatar: String?
)
