package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Favorito;
import com.tecnicsecure.entity.FavoritoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, FavoritoId> {

    List<Favorito> findByIdUsuario(Integer idUsuario);

    Optional<Favorito> findByIdUsuarioAndIdProducto(Integer idUsuario, Integer idProducto);

    void deleteByIdUsuarioAndIdProducto(Integer idUsuario, Integer idProducto);

    void deleteByIdUsuario(Integer idUsuario);
}
