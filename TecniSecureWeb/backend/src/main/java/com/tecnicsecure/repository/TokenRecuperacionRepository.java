/*
 * Este archivo sirve como el buscador que guarda, valida y limpia los códigos
 * temporales enviados para restablecer contraseñas olvidadas. Se utiliza en las
 * pantallas de Recuperar Contraseña y Restablecer Clave cuando una persona pide acceso.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Integer> {

    // Encuentra un código temporal que coincida con el ingresado y que todavía no haya sido usado
    Optional<TokenRecuperacion> findByTokenAndUsadoFalse(String token);

    // Borra todos los códigos temporales de recuperación que pertenezcan a una cuenta
    void deleteByIdUsuario(Integer idUsuario);
}
