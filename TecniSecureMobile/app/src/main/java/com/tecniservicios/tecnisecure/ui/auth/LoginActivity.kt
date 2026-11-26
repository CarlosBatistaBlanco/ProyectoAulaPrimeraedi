package com.tecniservicios.tecnisecure.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.local.SessionManager
import com.tecniservicios.tecnisecure.data.model.LoginRequest
import com.tecniservicios.tecnisecure.databinding.ActivityLoginBinding
import com.tecniservicios.tecnisecure.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var captchaToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Si ya hay sesión activa, ir directo al MainActivity
        if (sessionManager.isLoggedIn()) {
            irAMain()
            return
        }

        cargarCaptcha()

        binding.btnRecargarCaptcha.setOnClickListener {
            cargarCaptcha()
        }

        binding.btnLogin.setOnClickListener {
            ejecutarLogin()
        }

        binding.btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnConfigServidor.setOnClickListener {
            mostrarDialogoConfigServidor()
        }
    }

    private fun cargarCaptcha() {
        binding.tvCaptchaPregunta.text = "Cargando desafío..."
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@LoginActivity)
                val response = api.getCaptcha()
                if (response.isSuccessful && response.body() != null) {
                    val captcha = response.body()!!
                    val pregunta = captcha.pregunta.trim()
                    binding.tvCaptchaPregunta.text = if (pregunta.contains("?") || pregunta.contains("¿")) {
                        pregunta
                    } else {
                        "¿Cuánto es $pregunta?"
                    }
                    captchaToken = captcha.token
                    binding.etCaptcha.text?.clear()
                } else {
                    binding.tvCaptchaPregunta.text = "Error al cargar desafío. Pulsa recargar."
                }
            } catch (e: Exception) {
                binding.tvCaptchaPregunta.text = "Sin conexión al backend (${sessionManager.getBaseUrl()})"
            }
        }
    }

    private fun ejecutarLogin() {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val captcha = binding.etCaptcha.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty() || captcha.isEmpty()) {
            Toast.makeText(this, "Diligencia correo, contraseña y resultado del desafío", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Verificando..."

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@LoginActivity)
                val request = LoginRequest(correo, password, captcha, captchaToken)
                val response = api.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val tokenData = response.body()!!
                    sessionManager.saveTokens(tokenData.accessToken, tokenData.refreshToken)
                    sessionManager.saveUserData(
                        id = tokenData.usuario.idUsuario,
                        nombre = tokenData.usuario.nombre,
                        correo = tokenData.usuario.correo,
                        rol = tokenData.usuario.rol,
                        telefono = tokenData.usuario.telefono
                    )
                    Toast.makeText(this@LoginActivity, "¡Bienvenido ${tokenData.usuario.nombre}!", Toast.LENGTH_SHORT).show()
                    irAMain()
                } else {
                    val fallback = when (response.code()) {
                        400 -> "Resultado de la verificación matemática incorrecto"
                        401 -> "Correo o contraseña no válidos"
                        429, 423 -> "Cuenta bloqueada temporalmente por intentos fallidos"
                        else -> "Error de acceso (${response.code()})"
                    }
                    val errorMsg = ApiErrorUtils.extraerMensaje(response, fallback)
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    cargarCaptcha()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Falla de conexión con el backend: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                cargarCaptcha()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Iniciar Sesión"
            }
        }
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun mostrarDialogoConfigServidor() {
        val input = EditText(this).apply {
            setText(sessionManager.getBaseUrl())
            hint = "https://tecniservicios.onrender.com/"
        }

        AlertDialog.Builder(this)
            .setTitle("Servidor API TECNISECURE")
            .setMessage("Define la URL del backend Spring Boot:\n• Producción (Render): https://tecniservicios.onrender.com/\n• Emulador local: http://10.0.2.2:8080/")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaUrl = input.text.toString().trim()
                if (nuevaUrl.isNotEmpty()) {
                    sessionManager.setBaseUrl(nuevaUrl)
                    Toast.makeText(this, "Servidor actualizado", Toast.LENGTH_SHORT).show()
                    cargarCaptcha()
                }
            }
            .setNeutralButton("Usar Render") { _, _ ->
                sessionManager.setBaseUrl(SessionManager.DEFAULT_BASE_URL)
                Toast.makeText(this, "Conectado a Render en la nube", Toast.LENGTH_SHORT).show()
                cargarCaptcha()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
