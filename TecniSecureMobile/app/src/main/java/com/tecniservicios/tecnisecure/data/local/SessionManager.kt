package com.tecniservicios.tecnisecure.data.local

import android.content.Context
import android.content.SharedPreferences
import com.tecniservicios.tecnisecure.BuildConfig

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "tecnisecure_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_CHAT_TOKEN = "chat_token"
        private const val KEY_CHAT_ESCALADO = "chat_escalado"

        // URL por defecto: Render en producción para conectar desde cualquier celular o red
        const val DEFAULT_BASE_URL = "https://tecniservicios.onrender.com/"
        const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun saveUserData(id: Int, nombre: String, correo: String, rol: String, telefono: String? = null) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_EMAIL, correo)
            putString(KEY_USER_ROLE, rol)
            if (!telefono.isNullOrBlank()) {
                putString(KEY_USER_PHONE, telefono)
            }
            apply()
        }
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "CLIENTE") ?: "CLIENTE"

    fun esAdmin(): Boolean = getUserRole().contains("ADMIN", ignoreCase = true)
    fun esEmpleado(): Boolean = getUserRole().contains("EMPLEADO", ignoreCase = true)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        val finalUrl = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, finalUrl).apply()
    }

    fun getChatToken(): String? = prefs.getString(KEY_CHAT_TOKEN, null)
    fun setChatToken(token: String?) {
        prefs.edit().putString(KEY_CHAT_TOKEN, token).apply()
    }

    fun isChatEscalado(): Boolean = prefs.getBoolean(KEY_CHAT_ESCALADO, false)
    fun setChatEscalado(escalado: Boolean) {
        prefs.edit().putBoolean(KEY_CHAT_ESCALADO, escalado).apply()
    }

    fun limpiarChat() {
        prefs.edit().remove(KEY_CHAT_TOKEN).remove(KEY_CHAT_ESCALADO).apply()
    }

    // La API Key se define exclusivamente en local.properties (BuildConfig) y no es editable por usuarios
    fun getDeepSeekApiKey(): String = BuildConfig.DEEPSEEK_API_KEY.trim()
    fun tieneDeepSeekConfigurado(): Boolean = getDeepSeekApiKey().isNotBlank()

    fun logout() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHONE)
            remove(KEY_USER_ROLE)
            remove(KEY_CHAT_TOKEN)
            remove(KEY_CHAT_ESCALADO)
            apply()
        }
    }
}
