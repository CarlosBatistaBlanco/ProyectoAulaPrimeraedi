package com.tecnicsecure.repository;

import com.tecnicsecure.entity.EmpleadoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoPerfilRepository extends JpaRepository<EmpleadoPerfil, Integer> {

    Optional<EmpleadoPerfil> findByIdUsuario(Integer idUsuario);

    void deleteByIdUsuario(Integer idUsuario);
}
