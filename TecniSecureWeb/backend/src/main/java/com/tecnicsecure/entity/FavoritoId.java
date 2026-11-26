/*
 * Este archivo define la pareja única formada por una persona y un producto
 * para evitar que un mismo artículo se guarde dos veces como preferido. Se usa
 * internamente al agregar, consultar o quitar artículos en la sección de Favoritos.
 */
package com.tecnicsecure.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoritoId implements Serializable {

    // Números únicos de la persona y del producto que forman la pareja irrepetible
    private Integer idUsuario;
    private Integer idProducto;
}
