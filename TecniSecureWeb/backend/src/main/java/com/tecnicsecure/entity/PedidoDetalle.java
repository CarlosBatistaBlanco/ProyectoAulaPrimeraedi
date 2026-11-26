/*
 * Este archivo define cada renglón o artículo individual dentro de una compra,
 * guardando cuántas unidades se llevaron y el precio exacto que tenía en ese momento.
 * Esta información aparece en el resumen de Mis Pedidos, en las facturas y en reportes de ventas.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedido_detalle")
public class PedidoDetalle {

    // Número único que identifica cada renglón dentro de una compra
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    // Conexión con la compra principal y con el producto adquirido
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // Cantidad de unidades compradas y precio exacto por unidad al momento de la compra
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // Calcula el valor total de este renglón multiplicando el precio por la cantidad de unidades
    @Transient
    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
