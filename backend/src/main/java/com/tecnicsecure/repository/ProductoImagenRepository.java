package com.tecnicsecure.repository;

import com.tecnicsecure.entity.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Integer> {

    List<ProductoImagen> findByIdProductoOrderByOrdenAsc(Integer idProducto);
}
