/*
 * Este archivo define las acciones para organizar al equipo de trabajo: asignar areas,
 * decidir que secciones del panel puede ver cada empleado, repartir tareas y calificar sus
 * entregas. Se nota tanto en el panel del administrador como en el de cada empleado.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.entity.TareaHistorialEntrega;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EmpleadoGestionService {

    // Consulta el area, los permisos del menu y las actividades sugeridas del empleado conectado.
    Map<String, Object> miPerfil();

    // Entrega la lista de actividades tipicas ordenadas por cada area de la empresa.
    Map<String, List<Map<String, String>>> actividadesPorArea();

    // Muestra todos los empleados con sus tareas pendientes, terminadas y su nota promedio.
    List<Map<String, Object>> listarEmpleadosConCarga();

    // Cambia el area y las secciones del panel a las que puede entrar un empleado.
    Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos);

    // Cambia area y permisos del empleado y, si se pide, le crea de una vez las tareas de su cargo.
    Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos, boolean asignarActividadesRol);

    // Muestra al administrador todas las tareas asignadas a todo el equipo.
    List<Map<String, Object>> listarTodasTareas();

    // Muestra unicamente las tareas que tiene asignadas el empleado conectado.
    List<TareaEmpleado> misTareas();

    // Muestra paso a paso todas las entregas y revisiones que ha tenido una tarea.
    List<TareaHistorialEntrega> historialTarea(Integer idTarea);

    // Crea una tarea nueva y se la asigna a un empleado especifico con prioridad y fecha limite.
    TareaEmpleado asignarTarea(Integer idEmpleadoUsuario, String titulo, String descripcion, String prioridad, LocalDate fechaLimite);

    // Deja que el empleado reporte su avance, suba una evidencia y mande la tarea a revision.
    TareaEmpleado actualizarProgreso(Integer idTarea, String estado, String evidencia, String comentarioEmpleado);

    // Aprueba una tarea completada y le pone una calificacion de 1 a 5 estrellas.
    TareaEmpleado calificarTarea(Integer idTarea, Integer calificacion, String observacionAdmin);

    // Permite al administrador aprobar una entrega o devolverla al empleado para que haga correcciones.
    TareaEmpleado revisarYCalificarTarea(Integer idTarea, String estado, Integer calificacion, String observacionAdmin);

    // Arma el resumen de cumplimiento y calificaciones de todos los empleados.
    List<Map<String, Object>> reporteDesempeno();

    // Deja al administrador revisar el estado de una cuenta dejando constancia en el registro de control.
    Map<String, Object> accesoSoporteAuditoria(Integer idUsuario);
}
