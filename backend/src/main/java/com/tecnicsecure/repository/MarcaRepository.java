package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {

    Optional<Marca> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
