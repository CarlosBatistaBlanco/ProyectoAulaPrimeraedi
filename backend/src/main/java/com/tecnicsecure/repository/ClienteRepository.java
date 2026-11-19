package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCorreoIgnoreCase(String correo);

    Optional<Cliente> findByUsuarioIdUsuario(Integer idUsuario);
}
