/*
 * Este archivo define la ficha principal de cada compra realizada en la tienda o
 * registrada por un empleado, incluyendo el valor total, su etapa actual, la dirección
 * de entrega y el método de pago. Se usa en Mis Pedidos, en Ventas y en el panel de Pedidos.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {

    // Número único que identifica a cada compra o pedido
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    // Conexión con el cliente que hizo la compra y con el empleado que la registró en caja, si aplica
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado_registro")
    private Usuario empleadoRegistro;

    // Valor total exacto a pagar sin errores de centavos
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Etapa actual en la que se encuentra la compra (se guarda como texto para aceptar todas las etapas)
    @Column(name = "estado", length = 30)
    private String estado = EstadoPedido.PENDIENTE;

    // Dirección escrita y ubicación en el mapa donde se debe entregar la compra
    @Column(name = "direccion_entrega", length = 255)
    private String direccionEntrega;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Forma de pago elegida por el cliente y notas adicionales sobre ese pago
    @Column(name = "metodo_pago", length = 40)
    private String metodoPago;

    @Column(name = "detalle_pago", length = 200)
    private String detallePago;

    // Fecha y hora en que se realizó el pedido
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Lista de cada uno de los productos incluidos en esta compra
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoDetalle> detalles = new ArrayList<>();

    // Añade un producto a la lista de artículos de esta compra y deja ambos conectados
    public void agregarDetalle(PedidoDetalle detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }
}
