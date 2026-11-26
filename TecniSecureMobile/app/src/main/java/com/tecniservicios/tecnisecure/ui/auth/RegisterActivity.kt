package com.tecniservicios.tecnisecure.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecniservicios.tecnisecure.data.api.ApiErrorUtils
import com.tecniservicios.tecnisecure.data.api.RetrofitClient
import com.tecniservicios.tecnisecure.data.model.RegistroRequest
import com.tecniservicios.tecnisecure.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverLogin.setOnClickListener {
            finish()
        }

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etRegNombre.text.toString().trim()
            val documento = binding.etRegDocumento.text.toString().trim()
            val correo = binding.etRegCorreo.text.toString().trim()
            val telefono = binding.etRegTelefono.text.toString().trim().replace(" ", "").replace("-", "")
            val password = binding.etRegPassword.text.toString().trim()
            val direccion = binding.etRegDireccion.text.toString().trim()

            if (nombre.isEmpty() || documento.isEmpty() || correo.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!documento.matches(Regex("^[0-9]{5,15}$"))) {
                Toast.makeText(this, "El documento debe tener entre 5 y 15 dígitos numéricos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!telefono.matches(Regex("^(\\+?57)?[0-9]{7,10}$"))) {
                Toast.makeText(this, "El teléfono debe tener entre 7 y 10 dígitos numéricos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tieneMayus = password.any { it.isUpperCase() }
            val tieneMinus = password.any { it.isLowerCase() }
            val tieneNumero = password.any { it.isDigit() }
            if (password.length < 8 || !tieneMayus || !tieneMinus || !tieneNumero) {
                Toast.makeText(
                    this,
                    "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            binding.btnRegistrar.isEnabled = false
            binding.btnRegistrar.text = "Registrando..."

            lifecycleScope.launch {
                try {
                    val api = RetrofitClient.getApiService(this@RegisterActivity)
                    val request = RegistroRequest(
                        nombre = nombre,
                        documento = documento,
                        correo = correo,
                        telefono = telefono,
                        password = password,
                        direccion = direccion.ifEmpty { null }
                    )
                    val response = api.registro(request)

                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "¡Cuenta creada con éxito! Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val fallback = if (response.code() == 409) {
                            "El correo o documento ya se encuentran registrados"
                        } else {
                            "No se pudo completar el registro (${response.code()})"
                        }
                        val msg = ApiErrorUtils.extraerMensaje(response, fallback)
                        Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnRegistrar.isEnabled = true
                    binding.btnRegistrar.text = "Crear Cuenta"
                }
            }
        }
    }
}
