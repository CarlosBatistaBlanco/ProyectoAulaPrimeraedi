package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findTop30ByIdUsuarioOrderByCreadoEnDesc(Integer idUsuario);

    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);

    void deleteByIdUsuario(Integer idUsuario);
}
