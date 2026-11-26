/*
 * Este archivo define cada anotación del registro de entradas y salidas de bodega
 * para los productos de la tienda, indicando cuántas unidades cambiaron y cuántas
 * quedaron disponibles. Esta información se consulta en la sección de Inventario del panel.
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
@Table(name = "inventario_movimientos")
public class InventarioMovimiento {

    // Número único que identifica este movimiento de bodega
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    // Conexión con el producto afectado y con la persona del equipo que registró el cambio
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    // Detalle del cambio: si fue entrada o salida, cuántas unidades, existencias finales y explicación
    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(name = "motivo", length = 255)
    private String motivo;

    // Fecha y hora exacta en que se anotó el movimiento
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
