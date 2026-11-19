package com.tecnicsecure.controller;

import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.service.EmpleadoGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "9. Gestion de Empleados y Tareas", description = "Areas, actividades por rol, permisos, carga laboral, revision y desempeno")
@RestController
@RequestMapping("/api/gestion-empleados")
public class EmpleadoGestionController {

    private final EmpleadoGestionService service;

    public EmpleadoGestionController(EmpleadoGestionService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar mi area, modulos permitidos y actividades del rol")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/mi-perfil")
    public Map<String, Object> miPerfil() {
        return service.miPerfil();
    }

    @Operation(summary = "Consultar catalogo de actividades operativas por area/rol")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/areas-actividades")
    public Map<String, List<Map<String, String>>> areasActividades() {
        return service.actividadesPorArea();
    }

    @Operation(summary = "Consultar mis tareas diarias asignadas")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/mis-tareas")
    public List<TareaEmpleado> misTareas() {
        return service.misTareas();
    }

    @Operation(summary = "Actualizar avance y evidencia de una tarea propia")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @PatchMapping("/tareas/{id}/progreso")
    public TareaEmpleado actualizarProgreso(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return service.actualizarProgreso(id,
                body.get("estado"),
                body.get("evidencia"),
                body.get("comentarioEmpleado"));
    }

    @Operation(summary = "Listar empleados con su area, permisos y carga laboral activa")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/empleados")
    public List<Map<String, Object>> listarEmpleados() {
        return service.listarEmpleadosConCarga();
    }

    @Operation(summary = "Asignar area, modulos permitidos y opcionalmente crear actividades del rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SuppressWarnings("unchecked")
    @PutMapping("/empleados/{idUsuario}/permisos")
    public Map<String, Object> actualizarPermisos(@PathVariable Integer idUsuario, @RequestBody Map<String, Object> body) {
        String area = body.get("area") != null ? String.valueOf(body.get("area")) : "OPERATIVO";
        List<String> modulos = body.get("modulos") instanceof List<?> lista
                ? lista.stream().map(String::valueOf).toList()
                : List.of();
        boolean asignarActividadesRol = Boolean.TRUE.equals(body.get("asignarActividadesRol"));
        return service.actualizarPermisos(idUsuario, area, modulos, asignarActividadesRol);
    }

    @Operation(summary = "Listar todas las tareas de empleados")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/tareas")
    public List<Map<String, Object>> listarTareas() {
        return service.listarTodasTareas();
    }

    @Operation(summary = "Asignar nueva tarea diaria a un empleado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/tareas")
    public TareaEmpleado asignarTarea(@RequestBody Map<String, Object> body) {
        // Validacion explicita: un dato faltante debe dar 400, no un 500
        String idTexto = body.get("idEmpleadoUsuario") != null ? String.valueOf(body.get("idEmpleadoUsuario")) : "";
        Integer idEmp;
        try {
            idEmp = Integer.valueOf(idTexto.trim());
        } catch (NumberFormatException ex) {
            throw com.tecnicsecure.exception.ApiException.badRequest(
                    idTexto.isBlank() ? "Falta el empleado (idEmpleadoUsuario)"
                            : "El empleado indicado no es valido");
        }
        String titulo = String.valueOf(body.getOrDefault("titulo", "")).trim();
        if (titulo.isEmpty()) {
            throw com.tecnicsecure.exception.ApiException.badRequest("El titulo de la tarea es obligatorio");
        }
        String descripcion = body.get("descripcion") != null ? String.valueOf(body.get("descripcion")) : null;
        String prioridad = body.get("prioridad") != null ? String.valueOf(body.get("prioridad")) : "media";
        LocalDate fechaLimite = null;
        if (body.get("fechaLimite") != null && !String.valueOf(body.get("fechaLimite")).isBlank()) {
            try {
                fechaLimite = LocalDate.parse(String.valueOf(body.get("fechaLimite")));
            } catch (java.time.format.DateTimeParseException ex) {
                throw com.tecnicsecure.exception.ApiException.badRequest(
                        "La fecha limite debe tener el formato AAAA-MM-DD");
            }
        }
        return service.asignarTarea(idEmp, titulo, descripcion, prioridad, fechaLimite);
    }

    @Operation(summary = "Revisar evidencia y calificar (1 a 5) o devolver una tarea")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/tareas/{id}/calificar")
    public TareaEmpleado calificar(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String estado = body.get("estado") != null ? String.valueOf(body.get("estado")) : "revisada";
        Integer calificacion = body.get("calificacion") != null && !String.valueOf(body.get("calificacion")).isBlank()
                ? Integer.valueOf(String.valueOf(body.get("calificacion")))
                : null;
        String obs = body.get("observacionAdmin") != null ? String.valueOf(body.get("observacionAdmin")) : null;
        return service.revisarYCalificarTarea(id, estado, calificacion, obs);
    }

    @Operation(summary = "Reporte de cumplimiento y desempeno por empleado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/desempeno")
    public List<Map<String, Object>> desempeno() {
        return service.reporteDesempeno();
    }

    @Operation(summary = "Acceso maestro auditado para soporte tecnico sobre cuenta de usuario")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/acceso-soporte/{idUsuario}")
    public Map<String, Object> accesoSoporte(@PathVariable Integer idUsuario) {
        return service.accesoSoporteAuditoria(idUsuario);
    }
}
