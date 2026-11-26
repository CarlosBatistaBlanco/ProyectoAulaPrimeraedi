/*
 * Este archivo sirve como el buscador que consulta y guarda las marcas comerciales
 * de los productos vendidos en la tienda. Se utiliza al cargar los filtros del catálogo
 * público y al crear o editar productos y marcas en el panel administrativo.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {

    // Encuentra una marca buscando por su nombre exacto
    Optional<Marca> findByNombre(String nombre);

    // Comprueba si ya existe una marca guardada con ese mismo nombre para evitar duplicados
    boolean existsByNombre(String nombre);
}
