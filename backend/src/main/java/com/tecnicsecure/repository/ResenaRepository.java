package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    List<Resena> findByIdProductoOrderByCreadoEnDesc(Integer idProducto);

    Optional<Resena> findByIdProductoAndIdUsuario(Integer idProducto, Integer idUsuario);

    @Query("SELECT COALESCE(AVG(r.calificacion), 0) FROM Resena r WHERE r.idProducto = :idProducto")
    Double promedioPorProducto(@Param("idProducto") Integer idProducto);

    long countByIdProducto(Integer idProducto);
}
