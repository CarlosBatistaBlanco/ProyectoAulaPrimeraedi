/*
 * Este archivo sirve como el buscador que consulta, guarda y elimina la configuración
 * de área y permisos de pantallas de cada empleado. Se utiliza cuando un empleado entra
 * al panel y cuando el administrador define sus accesos en la sección de Usuarios.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.EmpleadoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoPerfilRepository extends JpaRepository<EmpleadoPerfil, Integer> {

    // Busca el área de trabajo y los permisos de pantallas configurados para un empleado
    Optional<EmpleadoPerfil> findByIdUsuario(Integer idUsuario);

    // Borra la configuración de permisos vinculada a una cuenta de empleado
    void deleteByIdUsuario(Integer idUsuario);
}
