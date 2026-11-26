/*
 * Este archivo define la ficha de información que el sistema guarda por cada artículo
 * de la tienda, como su nombre, precio, descuento, fotografía, garantía y unidades
 * disponibles. Aparece en el catálogo público, el carrito de compras y la sección de Productos e Inventario del panel.
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
@Table(name = "productos")
public class Producto {

    // Número único que identifica a cada artículo de la tienda
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    // Conexión con la categoría y la marca a las que pertenece el artículo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    // Nombre comercial, códigos de referencia en tienda y descripción detallada
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "sku", unique = true, length = 40)
    private String sku;

    @Column(name = "codigo_barras", unique = true, length = 40)
    private String codigoBarras;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Precio exacto sin errores de centavos, porcentaje de descuento, impuesto y meses de garantía
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "descuento_pct", precision = 5, scale = 2)
    private BigDecimal descuentoPct = BigDecimal.ZERO;

    @Column(name = "impuesto_pct", precision = 5, scale = 2)
    private BigDecimal impuestoPct = new BigDecimal("19.00");

    @Column(name = "garantia_meses")
    private Integer garantiaMeses = 12;

    // Control de bodega: unidades disponibles ahora, mínimo para avisar escasez y máximo permitido
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 5;

    @Column(name = "stock_maximo")
    private Integer stockMaximo = 100;

    // Fotografía principal, si aparece como destacado, si está visible u oculto en la tienda y fecha de registro
    @Column(name = "imagen", length = 255)
    private String imagen = "img/productos/sin-imagen.jpg";

    @Column(name = "destacado")
    private Boolean destacado = false;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Calcula el precio final que paga el cliente después de aplicar el descuento vigente
    @Transient
    public BigDecimal getPrecioFinal() {
        if (descuentoPct == null || descuentoPct.signum() <= 0) {
            return precio;
        }
        BigDecimal factor = BigDecimal.ONE.subtract(
                descuentoPct.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        return precio.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Indica si el artículo tiene actualmente un descuento mayor a cero
    @Transient
    public boolean isTieneDescuento() {
        return descuentoPct != null && descuentoPct.signum() > 0;
    }

    // Indica si las unidades disponibles llegaron al mínimo de alerta o están por debajo
    @Transient
    public boolean isStockBajo() {
        return stock != null && stockMinimo != null && stock <= stockMinimo;
    }
}
