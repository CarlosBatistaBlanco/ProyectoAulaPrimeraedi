/*
 * Este archivo sirve como el buscador que consulta y guarda la información de los
 * clientes registrados en la tienda. Se utiliza cuando un comprador realiza un pedido,
 * actualiza su perfil o cuando el equipo revisa la sección de Clientes y Ventas del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Busca a un cliente por su correo electrónico sin importar mayúsculas o minúsculas
    Optional<Cliente> findByCorreoIgnoreCase(String correo);

    // Encuentra la ficha de cliente conectada con una cuenta de acceso concreta
    Optional<Cliente> findByUsuarioIdUsuario(Integer idUsuario);
}
