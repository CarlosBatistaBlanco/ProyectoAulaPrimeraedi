package com.tecnicsecure.repository;

import com.tecnicsecure.entity.DireccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionUsuarioRepository extends JpaRepository<DireccionUsuario, Integer> {

    List<DireccionUsuario> findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(Integer idUsuario);

    Optional<DireccionUsuario> findByIdDireccionAndIdUsuario(Integer idDireccion, Integer idUsuario);

    Optional<DireccionUsuario> findFirstByIdUsuarioAndPredeterminadaTrue(Integer idUsuario);

    long countByIdUsuario(Integer idUsuario);

    void deleteByIdUsuario(Integer idUsuario);
}
