/*
 * Este archivo define la ficha con los datos de contacto de cada persona que se
 * registra para comprar en la tienda y la conexión con su cuenta de acceso. Esta
 * información se utiliza en el perfil del comprador, en los pedidos y en la sección de Clientes del panel.
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
@Table(name = "clientes")
public class Cliente {

    // Número único que identifica a cada cliente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    // Conexión con la cuenta con la que esta persona inicia sesión
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    // Nombre completo y datos de contacto del cliente
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "direccion", length = 255)
    private String direccion;

    // Fecha y hora en que el cliente se registró en la tienda
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
