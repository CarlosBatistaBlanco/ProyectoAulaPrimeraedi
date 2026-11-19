package com.tecnicsecure.repository;

import com.tecnicsecure.entity.SolicitudServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicio, Integer> {

    Page<SolicitudServicio> findByEstadoOrderByCreadoEnDesc(String estado, Pageable pageable);

    long countByEstado(String estado);
}
