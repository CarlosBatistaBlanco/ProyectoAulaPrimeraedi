package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Integer> {

    Optional<TokenRecuperacion> findByTokenAndUsadoFalse(String token);

    void deleteByIdUsuario(Integer idUsuario);
}
