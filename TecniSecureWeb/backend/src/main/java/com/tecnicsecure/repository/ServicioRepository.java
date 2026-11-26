/*
 * Este archivo sirve como el buscador que consulta y guarda los servicios técnicos
 * que ofrece la empresa. Se utiliza al cargar las tarjetas informativas en la página
 * pública de Servicios y al desplegar la lista de opciones en el formulario de cotización.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    // Trae todos los servicios disponibles respetando el orden definido para mostrarlos en la página
    List<Servicio> findAllByOrderByOrdenAsc();
}
