/*
 * Este archivo define la ficha de cada dirección de entrega que una persona guarda en su
 * cuenta, como su casa, oficina o bodega. Se utiliza en la sección Mis Direcciones del
 * perfil y para llenar automáticamente el domicilio al finalizar una compra.
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
@Table(name = "direcciones_usuario")
public class DireccionUsuario {

    // Número único de la dirección y conexión con la persona a la que pertenece
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Integer idDireccion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    // Nombre corto para reconocerla fácilmente, como "Casa", "Oficina" o "Bodega"
    @Column(name = "etiqueta", nullable = false, length = 60)
    private String etiqueta;

    // Datos completos del domicilio y de la persona encargada de recibir el pedido
    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "barrio", length = 100)
    private String barrio;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "nombre_recibe", length = 120)
    private String nombreRecibe;

    @Column(name = "referencia", length = 255)
    private String referencia;

    // Ubicación en el mapa para facilitar la entrega
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Indica si esta es la dirección principal elegida por defecto para las compras
    @Column(name = "predeterminada", nullable = false)
    private Boolean predeterminada = false;

    // Fechas en que se guardó la dirección y en que se editó por última vez
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
