/*
 * Este archivo sirve como el buscador que guarda y consulta las fotografías adicionales
 * que forman la galería de cada artículo. Se utiliza al abrir el detalle de un producto
 * en la tienda y al administrar sus fotos en la sección de Productos del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Integer> {

    // Trae todas las fotos adicionales de un producto respetando el orden configurado para la galería
    List<ProductoImagen> findByIdProductoOrderByOrdenAsc(Integer idProducto);
}
