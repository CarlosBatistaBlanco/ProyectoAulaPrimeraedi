/*
 * Este archivo sirve como el buscador que consulta, guarda y elimina las categorías
 * en las que se agrupan los productos de la tienda. Se utiliza al cargar los filtros
 * del catálogo público y al administrar las categorías desde el panel de Productos.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}
