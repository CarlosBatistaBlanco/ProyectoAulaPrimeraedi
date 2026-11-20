package com.tecnicsecure.service;

import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.entity.TareaHistorialEntrega;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EmpleadoGestionService {

    Map<String, Object> miPerfil();

    Map<String, List<Map<String, String>>> actividadesPorArea();

    List<Map<String, Object>> listarEmpleadosConCarga();

    Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos);

    Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos, boolean asignarActividadesRol);

    List<Map<String, Object>> listarTodasTareas();

    List<TareaEmpleado> misTareas();

    List<TareaHistorialEntrega> historialTarea(Integer idTarea);

    TareaEmpleado asignarTarea(Integer idEmpleadoUsuario, String titulo, String descripcion, String prioridad, LocalDate fechaLimite);

    TareaEmpleado actualizarProgreso(Integer idTarea, String estado, String evidencia, String comentarioEmpleado);

    TareaEmpleado calificarTarea(Integer idTarea, Integer calificacion, String observacionAdmin);

    TareaEmpleado revisarYCalificarTarea(Integer idTarea, String estado, Integer calificacion, String observacionAdmin);

    List<Map<String, Object>> reporteDesempeno();

    Map<String, Object> accesoSoporteAuditoria(Integer idUsuario);
}
