/*
 * Este archivo sirve como el buscador que consulta y guarda la ficha laboral de cada
 * empleado de la empresa. Se utiliza en la sección de Empleados y Usuarios del panel
 * administrativo y al validar si una cuenta pertenece a un integrante del equipo.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    // Encuentra la ficha laboral de un empleado a partir de su cuenta de acceso
    Optional<Empleado> findByUsuarioIdUsuario(Integer idUsuario);

    // Comprueba si una cuenta de acceso ya tiene creada su ficha como empleado
    boolean existsByUsuarioIdUsuario(Integer idUsuario);
}
