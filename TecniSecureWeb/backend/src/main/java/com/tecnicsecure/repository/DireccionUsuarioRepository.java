/*
 * Este archivo sirve como el buscador que guarda, consulta y elimina las direcciones
 * de entrega de cada persona registrada. Se utiliza en la sección Mis Direcciones del
 * perfil y al sugerir la dirección principal durante el proceso de compra.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.DireccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionUsuarioRepository extends JpaRepository<DireccionUsuario, Integer> {

    // Trae todas las direcciones guardadas por una persona, mostrando primero la principal
    List<DireccionUsuario> findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(Integer idUsuario);

    // Busca una dirección puntual asegurándose de que realmente pertenezca a la persona indicada
    Optional<DireccionUsuario> findByIdDireccionAndIdUsuario(Integer idDireccion, Integer idUsuario);

    // Obtiene la dirección marcada como principal para llenar automáticamente una compra
    Optional<DireccionUsuario> findFirstByIdUsuarioAndPredeterminadaTrue(Integer idUsuario);

    // Cuenta cuántas direcciones tiene guardadas una persona en su cuenta
    long countByIdUsuario(Integer idUsuario);

    // Borra todas las direcciones guardadas por una persona
    void deleteByIdUsuario(Integer idUsuario);
}
