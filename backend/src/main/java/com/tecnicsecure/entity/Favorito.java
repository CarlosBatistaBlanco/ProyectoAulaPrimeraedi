package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Favorito: un cliente/usuario marca productos de interes.
 * Clave primaria compuesta (id_usuario, id_producto) tal como en el esquema original.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "favoritos")
@IdClass(FavoritoId.class)
public class Favorito {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
