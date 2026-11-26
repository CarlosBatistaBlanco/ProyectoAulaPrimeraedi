/*
 * Este archivo define la ficha de cada comprobante de factura emitido para un pedido,
 * incluyendo su número visible, los valores cobrados, los descuentos y su estado actual.
 * Esta información se muestra en la sección de Facturación del panel y al descargar recibos.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "facturas")
public class Factura {

    // Número único interno, pedido al que corresponde y número visible del comprobante
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Integer idFactura;

    @Column(name = "id_pedido", nullable = false, unique = true)
    private Integer idPedido;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 20)
    private String numeroFactura;

    // Valores económicos exactos sin errores de centavos: subtotal, impuestos, descuento y total
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "impuestos", nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // Estado del comprobante (como emitida o anulada) y fecha en que se generó
    @Column(name = "estado", length = 20)
    private String estado = "emitida";

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
