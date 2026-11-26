/*
 * Este archivo sirve como el buscador que guarda, consulta y elimina las tareas
 * asignadas al personal de la empresa. Se utiliza en la sección de Tareas del panel
 * del empleado para ver sus pendientes y en el panel del administrador para supervisarlas.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TareaEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TareaEmpleadoRepository extends JpaRepository<TareaEmpleado, Integer> {

    // Trae todas las tareas asignadas a un empleado concreto, de la más reciente a la más antigua
    List<TareaEmpleado> findByIdEmpleadoUsuarioOrderByCreadoEnDesc(Integer idEmpleadoUsuario);

    // Trae todas las tareas de todo el equipo ordenadas desde las creadas más recientemente
    List<TareaEmpleado> findAllByOrderByCreadoEnDesc();

    // Cuenta cuántas tareas tiene un empleado en ciertos estados, por ejemplo pendientes o en proceso
    long countByIdEmpleadoUsuarioAndEstadoIn(Integer idEmpleadoUsuario, Collection<String> estados);

    // Borra todas las tareas asignadas a un empleado específico
    void deleteByIdEmpleadoUsuario(Integer idEmpleadoUsuario);
}
