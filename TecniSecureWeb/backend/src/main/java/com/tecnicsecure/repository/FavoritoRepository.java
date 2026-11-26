/*
 * Este archivo sirve como el buscador que guarda, consulta y quita los productos que
 * cada persona marca como preferidos en la tienda. Se utiliza al tocar el corazón en
 * el catálogo de productos y al mostrar la lista personal en la sección de Favoritos.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Favorito;
import com.tecnicsecure.entity.FavoritoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, FavoritoId> {

    // Trae todos los productos que una persona tiene guardados en su lista de favoritos
    List<Favorito> findByIdUsuario(Integer idUsuario);

    // Revisa si una persona ya había marcado un producto específico como favorito
    Optional<Favorito> findByIdUsuarioAndIdProducto(Integer idUsuario, Integer idProducto);

    // Quita un producto puntual de la lista de favoritos de una persona
    void deleteByIdUsuarioAndIdProducto(Integer idUsuario, Integer idProducto);

    // Borra todos los productos favoritos guardados por una persona
    void deleteByIdUsuario(Integer idUsuario);
}
