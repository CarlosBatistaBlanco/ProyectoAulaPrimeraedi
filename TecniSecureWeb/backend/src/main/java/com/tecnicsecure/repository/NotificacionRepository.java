/*
 * Este archivo sirve como el buscador que guarda, consulta y elimina los avisos
 * recibidos por cada persona dentro de la plataforma. Se utiliza en la campana de
 * Notificaciones de la barra superior para mostrar las alertas recientes y cuántas faltan por leer.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Trae los 30 avisos más recientes de una persona, del último hacia atrás
    List<Notificacion> findTop30ByIdUsuarioOrderByCreadoEnDesc(Integer idUsuario);

    // Cuenta cuántos avisos sin leer tiene una persona para mostrar el globito de la campana
    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);

    // Borra todos los avisos guardados de una persona
    void deleteByIdUsuario(Integer idUsuario);
}
