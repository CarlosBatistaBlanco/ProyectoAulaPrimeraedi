package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByUsuarioIdUsuario(Integer idUsuario);

    boolean existsByUsuarioIdUsuario(Integer idUsuario);
}
