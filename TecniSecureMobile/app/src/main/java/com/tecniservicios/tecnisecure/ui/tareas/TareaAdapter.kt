package com.tecniservicios.tecnisecure.ui.tareas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tecniservicios.tecnisecure.data.model.TareaEmpleado
import com.tecniservicios.tecnisecure.databinding.ItemTareaBinding

class TareaAdapter(
    private var tareas: List<TareaEmpleado>,
    private val esModoAdmin: Boolean = false,
    private val onAccionPrincipalClick: (TareaEmpleado) -> Unit,
    private val onHistorialClick: (TareaEmpleado) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    fun actualizarLista(nuevas: List<TareaEmpleado>) {
        tareas = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    inner class TareaViewHolder(private val binding: ItemTareaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: TareaEmpleado) {
            binding.tvTareaTitulo.text = tarea.titulo
            binding.tvTareaPrioridad.text = tarea.prioridad.uppercase()
            binding.tvTareaEstado.text = tarea.estado.replace("_", " ").uppercase()
            binding.tvTareaDescripcion.text = tarea.descripcion ?: "Sin descripción detallada"

            when (tarea.prioridad.lowercase()) {
                "alta", "urgente" -> binding.tvTareaPrioridad.setBackgroundColor(Color.parseColor("#FF5A36"))
                "media" -> binding.tvTareaPrioridad.setBackgroundColor(Color.parseColor("#006B85"))
                else -> binding.tvTareaPrioridad.setBackgroundColor(Color.parseColor("#64748B"))
            }

            // Mostrar responsable y fecha límite
            val metaPartes = mutableListOf<String>()
            if (!tarea.empleadoNombre.isNullOrBlank()) {
                metaPartes.add("Técnico: ${tarea.empleadoNombre}")
            }
            if (!tarea.fechaLimite.isNullOrBlank()) {
                metaPartes.add("Límite: ${tarea.fechaLimite}")
            }
            if ((tarea.totalEntregas ?: 0) > 0) {
                metaPartes.add("Entregas: ${tarea.totalEntregas}")
            }
            if (metaPartes.isNotEmpty()) {
                binding.tvTareaResponsable.visibility = View.VISIBLE
                binding.tvTareaResponsable.text = metaPartes.joinToString(" · ")
            } else {
                binding.tvTareaResponsable.visibility = View.GONE
            }

            val estadoLower = tarea.estado.lowercase()

            if (esModoAdmin) {
                binding.btnEntregarTarea.isEnabled = true
                binding.btnEntregarTarea.alpha = 1.0f
                binding.btnEntregarTarea.text = if (estadoLower == "completada") {
                    "Revisar / Calificar"
                } else {
                    " Evaluar / Gestionar"
                }

                when {
                    estadoLower == "completada" -> {
                        binding.tvAvisoBloqueo.visibility = View.VISIBLE
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#9A3412"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#FFEDD5"))
                        binding.tvAvisoBloqueo.text = "Entrega lista para revisión · Evidencia: ${tarea.evidencia ?: "Adjunta"}"
                    }
                    estadoLower == "revisada" -> {
                        binding.tvAvisoBloqueo.visibility = View.VISIBLE
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#065F46"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#D1FAE5"))
                        binding.tvAvisoBloqueo.text = "Aprobada (${tarea.calificacion ?: 5}/5) ${tarea.observacionAdmin?.let { "· $it" } ?: ""}"
                    }
                    !tarea.observacionAdmin.isNullOrBlank() -> {
                        binding.tvAvisoBloqueo.visibility = View.VISIBLE
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#B45309"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#FEF3C7"))
                        binding.tvAvisoBloqueo.text = "Devuelta para corrección: ${tarea.observacionAdmin}"
                    }
                    else -> binding.tvAvisoBloqueo.visibility = View.GONE
                }
            } else {
                // Modo EMPLEADO: Bloqueo estricto v2.0 si ya fue enviada ("completada") o cerrada ("revisada")
                val estaBloqueada = estadoLower == "completada" ||
                        estadoLower == "en_revision" ||
                        estadoLower == "revisada" ||
                        estadoLower == "bloqueada"

                if (estaBloqueada) {
                    binding.tvAvisoBloqueo.visibility = View.VISIBLE
                    if (estadoLower == "revisada") {
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#065F46"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#D1FAE5"))
                        binding.tvAvisoBloqueo.text = "Aprobada y cerrada (${tarea.calificacion ?: 5}/5). Solo lectura."
                    } else {
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#B91C1C"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#FEE2E2"))
                        binding.tvAvisoBloqueo.text = "Bloqueada: En revisión por el administrador. Solo lectura."
                    }
                    binding.btnEntregarTarea.text = "Bloqueada"
                    binding.btnEntregarTarea.isEnabled = false
                    binding.btnEntregarTarea.alpha = 0.5f
                } else {
                    if (!tarea.observacionAdmin.isNullOrBlank()) {
                        binding.tvAvisoBloqueo.visibility = View.VISIBLE
                        binding.tvAvisoBloqueo.setTextColor(Color.parseColor("#B45309"))
                        binding.tvAvisoBloqueo.setBackgroundColor(Color.parseColor("#FEF3C7"))
                        binding.tvAvisoBloqueo.text = "Corrección solicitada por Admin: ${tarea.observacionAdmin}"
                    } else {
                        binding.tvAvisoBloqueo.visibility = View.GONE
                    }
                    binding.btnEntregarTarea.text = "Entregar con Evidencia"
                    binding.btnEntregarTarea.isEnabled = true
                    binding.btnEntregarTarea.alpha = 1.0f
                }
            }

            binding.btnEntregarTarea.setOnClickListener {
                onAccionPrincipalClick(tarea)
            }

            binding.btnHistorialTarea.setOnClickListener {
                onHistorialClick(tarea)
            }
        }
    }
}
