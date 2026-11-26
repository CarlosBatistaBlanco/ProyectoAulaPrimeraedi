/*
 * Este archivo sirve como el buscador que guarda y consulta las opiniones y
 * calificaciones en estrellas de cada producto. Se utiliza en la página de detalle
 * del artículo para mostrar comentarios, evitar opiniones repetidas y calcular el promedio.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    // Trae todas las opiniones de un producto, de la más reciente a la más antigua
    List<Resena> findByIdProductoOrderByCreadoEnDesc(Integer idProducto);

    // Revisa si una persona ya dejó una opinión sobre un producto específico
    Optional<Resena> findByIdProductoAndIdUsuario(Integer idProducto, Integer idUsuario);

    // Calcula el promedio de estrellas que ha recibido un producto (o 0 si todavía no tiene opiniones)
    @Query("SELECT COALESCE(AVG(r.calificacion), 0) FROM Resena r WHERE r.idProducto = :idProducto")
    Double promedioPorProducto(@Param("idProducto") Integer idProducto);

    // Cuenta cuántas opiniones en total tiene un producto
    long countByIdProducto(Integer idProducto);
}
