/*
 * Este archivo define la ficha que guarda las marcas comerciales de los equipos y
 * productos que ofrece la tienda, como Hikvision, Dahua o Ezviz. Esta información
 * se utiliza en los filtros del catálogo público y en la sección de Productos y Marcas del panel.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "marcas")
public class Marca {

    // Número único que identifica a cada marca comercial
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marca")
    private Integer idMarca;

    // Nombre comercial único con el que se muestra la marca
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
