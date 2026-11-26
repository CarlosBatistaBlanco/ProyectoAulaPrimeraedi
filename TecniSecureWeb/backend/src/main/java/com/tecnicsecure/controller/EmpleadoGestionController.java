/*
 * Este archivo atiende las pantallas de Gestión de Empleados y Tareas en los paneles de
 * administrador y empleado. Permite al jefe asignar áreas, elegir qué secciones del panel
 * ve cada trabajador, crear tareas y calificar su desempeño, mientras el empleado reporta avances.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.entity.TareaHistorialEntrega;
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

    // Conecta con el encargado de administrar permisos, tareas y calificaciones del personal
    public EmpleadoGestionController(EmpleadoGestionService service) {
        this.service = service;
    }

    // Entrega al empleado su área de trabajo, las secciones que puede abrir en el menú y sus funciones
    @Operation(summary = "Consultar mi area, modulos permitidos y actividades del rol")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/mi-perfil")
    public Map<String, Object> miPerfil() {
        return service.miPerfil();
    }

    // Muestra la lista de actividades recomendadas para cada área de la empresa
    @Operation(summary = "Consultar catalogo de actividades operativas por area/rol")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/areas-actividades")
    public Map<String, List<Map<String, String>>> areasActividades() {
        return service.actividadesPorArea();
    }

    // Muestra las tareas del día asignadas a la persona que tiene sesión abierta
    @Operation(summary = "Consultar mis tareas diarias asignadas")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/mis-tareas")
    public List<TareaEmpleado> misTareas() {
        return service.misTareas();
    }

    // Guarda el avance, foto o evidencia y nota que envía el empleado sobre una de sus tareas
    @Operation(summary = "Actualizar avance y evidencia de una tarea propia")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @PatchMapping("/tareas/{id}/progreso")
    public TareaEmpleado actualizarProgreso(@PathVariable Integer id,
                                            @RequestBody(required = false) Map<String, String> body) {
        // Avisa si el formulario llegó sin datos antes de intentar guardar
        if (body == null) {
            throw com.tecnicsecure.exception.ApiException.badRequest("Debes enviar los datos de avance de la tarea");
        }
        return service.actualizarProgreso(id,
                body.get("estado"),
                body.get("evidencia"),
                body.get("comentarioEmpleado"));
    }

    // Muestra el archivo histórico de entregas anteriores y correcciones de una tarea
    @Operation(summary = "Consultar historial archivado de entregas y revisiones de una tarea")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @GetMapping("/tareas/{id}/historial")
    public List<TareaHistorialEntrega> historialTarea(@PathVariable Integer id) {
        return service.historialTarea(id);
    }

    // Muestra la tabla de todos los empleados con su área, permisos y cantidad de tareas activas (solo administrador)
    @Operation(summary = "Listar empleados con su area, permisos y carga laboral activa")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/empleados")
    public List<Map<String, Object>> listarEmpleados() {
        return service.listarEmpleadosConCarga();
    }

    // Cambia el área de un trabajador, sus secciones visibles del panel y si recibe tareas base (solo administrador)
    @Operation(summary = "Asignar area, modulos permitidos y opcionalmente crear actividades del rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SuppressWarnings("unchecked")
    @PutMapping("/empleados/{idUsuario}/permisos")
    public Map<String, Object> actualizarPermisos(@PathVariable Integer idUsuario,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            throw com.tecnicsecure.exception.ApiException.badRequest("Debes indicar area y modulos del empleado");
        }
        // Lee el área elegida (OPERATIVO si no se indicó otra), las secciones marcadas y la opción de tareas base
        String area = body.get("area") != null ? String.valueOf(body.get("area")) : "OPERATIVO";
        List<String> modulos = body.get("modulos") instanceof List<?> lista
                ? lista.stream().map(String::valueOf).toList()
                : List.of();
        boolean asignarActividadesRol = Boolean.TRUE.equals(body.get("asignarActividadesRol"));
        return service.actualizarPermisos(idUsuario, area, modulos, asignarActividadesRol);
    }

    // Lista todas las tareas de todo el equipo para que el administrador haga seguimiento
    @Operation(summary = "Listar todas las tareas de empleados")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/tareas")
    public List<Map<String, Object>> listarTareas() {
        return service.listarTodasTareas();
    }

    // Crea y asigna una nueva tarea a un empleado desde el panel de administración
    @Operation(summary = "Asignar nueva tarea diaria a un empleado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/tareas")
    public TareaEmpleado asignarTarea(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            throw com.tecnicsecure.exception.ApiException.badRequest("Los datos de la tarea son obligatorios");
        }
        // Comprueba que se haya elegido un empleado válido para mostrar un aviso claro si falta
        String idTexto = body.get("idEmpleadoUsuario") != null ? String.valueOf(body.get("idEmpleadoUsuario")) : "";
        Integer idEmp;
        try {
            idEmp = Integer.valueOf(idTexto.trim());
        } catch (NumberFormatException ex) {
            throw com.tecnicsecure.exception.ApiException.badRequest(
                    idTexto.isBlank() ? "Falta el empleado (idEmpleadoUsuario)"
                            : "El empleado indicado no es valido");
        }
        // Revisa que la tarea tenga título y verifica el formato de la fecha límite si se escribió
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

    // Permite al administrador revisar la evidencia, calificar de 1 a 5 estrellas o devolver la tarea con comentarios
    @Operation(summary = "Revisar evidencia y calificar (1 a 5) o devolver una tarea")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/tareas/{id}/calificar")
    public TareaEmpleado calificar(@PathVariable Integer id,
                                   @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            throw com.tecnicsecure.exception.ApiException.badRequest("Debes enviar los datos de revision de la tarea");
        }
        // Toma la decisión de revisión, verifica que la nota sea un número entero y guarda el comentario
        String estado = body.get("estado") != null ? String.valueOf(body.get("estado")) : "revisada";
        Integer calificacion = null;
        if (body.get("calificacion") != null && !String.valueOf(body.get("calificacion")).isBlank()) {
            try {
                calificacion = Integer.valueOf(String.valueOf(body.get("calificacion")).trim());
            } catch (NumberFormatException ex) {
                throw com.tecnicsecure.exception.ApiException.badRequest(
                        "La calificacion debe ser un numero entero entre 1 y 5");
            }
        }
        String obs = body.get("observacionAdmin") != null ? String.valueOf(body.get("observacionAdmin")) : null;
        return service.revisarYCalificarTarea(id, estado, calificacion, obs);
    }

    // Entrega el resumen de cumplimiento y promedio de calificaciones de cada trabajador (solo administrador)
    @Operation(summary = "Reporte de cumplimiento y desempeno por empleado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/desempeno")
    public List<Map<String, Object>> desempeno() {
        return service.reporteDesempeno();
    }

    // Permite al administrador revisar la ficha de una cuenta para dar soporte dejando registro de su visita
    @Operation(summary = "Acceso maestro auditado para soporte tecnico sobre cuenta de usuario")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/acceso-soporte/{idUsuario}")
    public Map<String, Object> accesoSoporte(@PathVariable Integer idUsuario) {
        return service.accesoSoporteAuditoria(idUsuario);
    }
}
