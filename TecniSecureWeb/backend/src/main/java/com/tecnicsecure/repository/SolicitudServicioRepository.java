/*
 * Este archivo sirve como el buscador que guarda y consulta las solicitudes de
 * cotización o visita técnica enviadas desde la web. Se utiliza al recibir nuevas
 * peticiones en Servicios y cuando el equipo las revisa y organiza en Solicitudes del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.SolicitudServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicio, Integer> {

    // Busca por páginas las solicitudes en un estado concreto para no cargar todo a la vez, de la más nueva a la más antigua
    Page<SolicitudServicio> findByEstadoOrderByCreadoEnDesc(String estado, Pageable pageable);

    // Cuenta cuántas solicitudes de servicio se encuentran en un estado determinado
    long countByEstado(String estado);
}
