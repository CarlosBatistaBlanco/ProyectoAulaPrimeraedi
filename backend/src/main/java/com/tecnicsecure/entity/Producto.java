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
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "sku", unique = true, length = 40)
    private String sku;

    @Column(name = "codigo_barras", unique = true, length = 40)
    private String codigoBarras;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "descuento_pct", precision = 5, scale = 2)
    private BigDecimal descuentoPct = BigDecimal.ZERO;

    @Column(name = "impuesto_pct", precision = 5, scale = 2)
    private BigDecimal impuestoPct = new BigDecimal("19.00");

    @Column(name = "garantia_meses")
    private Integer garantiaMeses = 12;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 5;

    @Column(name = "stock_maximo")
    private Integer stockMaximo = 100;

    @Column(name = "imagen", length = 255)
    private String imagen = "img/productos/sin-imagen.jpg";

    @Column(name = "destacado")
    private Boolean destacado = false;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    /** Precio final aplicando el descuento vigente (lo que paga el cliente). */
    @Transient
    public BigDecimal getPrecioFinal() {
        if (descuentoPct == null || descuentoPct.signum() <= 0) {
            return precio;
        }
        BigDecimal factor = BigDecimal.ONE.subtract(
                descuentoPct.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        return precio.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isTieneDescuento() {
        return descuentoPct != null && descuentoPct.signum() > 0;
    }

    @Transient
    public boolean isStockBajo() {
        return stock != null && stockMinimo != null && stock <= stockMinimo;
    }
}
