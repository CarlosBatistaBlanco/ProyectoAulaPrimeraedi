/*
 * Este archivo define la ficha que guarda las categorías usadas para organizar
 * los productos de la tienda, como cámaras, alarmas o accesorios. Esta información
 * se muestra en los filtros del catálogo público y en la sección de Categorías del panel.
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
@Table(name = "categorias")
public class Categoria {

    // Número único que identifica a cada categoría
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    // Nombre con el que aparece la categoría en la tienda
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;
}
