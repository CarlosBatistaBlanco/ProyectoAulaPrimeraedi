/*
 * Este archivo define el registro que guarda cada producto que una persona marca con
 * corazón o como preferido para verlo después. Esta información alimenta la lista de
 * Favoritos del cliente y los íconos de guardado en el catálogo de la tienda.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "favoritos")
@IdClass(FavoritoId.class)
public class Favorito {

    // Combinación única formada por la persona que guarda el producto y el producto elegido
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    // Fecha y hora en que la persona agregó el producto a su lista de favoritos
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
