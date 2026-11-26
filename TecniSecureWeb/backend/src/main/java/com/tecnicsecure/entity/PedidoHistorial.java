/*
 * Este archivo define cada anotación de la línea de tiempo de una compra, guardando
 * de qué etapa pasó a cuál, quién hizo el cambio y en qué momento ocurrió. Se muestra
 * en el seguimiento de Mis Pedidos y en el detalle de cada orden en el panel de Pedidos.
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
@Table(name = "pedidos_historial")
public class PedidoHistorial {

    // Número único del registro y conexión con el pedido al que pertenece
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @Column(name = "id_pedido", nullable = false)
    private Integer idPedido;

    // Etapa en la que estaba la compra y nueva etapa a la que avanzó
    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private String estadoNuevo;

    // Persona del equipo que hizo la actualización y fecha en que quedó registrada
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
