/*
 * Este archivo sirve como el buscador que consulta los tipos de perfil disponibles
 * en la plataforma (administrador, empleado y cliente). Se utiliza al crear cuentas
 * nuevas en el registro público y al asignar o cambiar perfiles en Usuarios del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    // Encuentra un tipo de perfil buscándolo por su nombre exacto (administrador, empleado o cliente)
    Optional<Rol> findByNombre(String nombre);
}
