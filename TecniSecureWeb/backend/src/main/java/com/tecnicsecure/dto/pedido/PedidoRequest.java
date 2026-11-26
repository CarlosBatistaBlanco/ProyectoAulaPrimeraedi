/*
 * Este archivo agrupa toda la información que envía un cliente al confirmar su compra
 * en la pantalla del carrito: sus datos de contacto, la dirección de envío, la forma
 * de pago y la lista de productos elegidos con sus cantidades.
 */
package com.tecnicsecure.dto.pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Checkout del carrito de compras")
public record PedidoRequest(
        // Nombre completo obligatorio del comprador (máximo 120 caracteres, solo letras y espacios)
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios") String nombre,
        // Correo electrónico obligatorio y válido donde recibirá la confirmación y la factura
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com") String correo,
        // Teléfono de contacto; solo números y entre 7 y 10 dígitos
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)") String telefono,
        // Dirección donde se entregará el pedido (máximo 255 caracteres)
        @Size(max = 255) String direccion,
        // Lista de productos del carrito; debe incluir al menos un artículo y cada línea se valida por separado
        @NotEmpty(message = "El carrito esta vacio")
        @Valid
        List<ItemCarrito> carrito,
        // Medio de pago elegido y detalle adicional, como la referencia de la transacción
        @Size(max = 40) String metodoPago,
        @Size(max = 200) String detallePago,
        // Ubicación opcional marcada en el mapa para facilitar la entrega
        Double latitud,
        Double longitud
) {

    // Permite armar una solicitud de pedido sencilla cuando no se especifican método de pago ni coordenadas
    public PedidoRequest(String nombre, String correo, String telefono, String direccion, List<ItemCarrito> carrito) {
        this(nombre, correo, telefono, direccion, carrito, null, null, null, null);
    }

    // Representa cada producto y la cantidad deseada dentro del carrito de compras
    @Schema(description = "Linea del carrito")
    public record ItemCarrito(
            // Identificador obligatorio del producto seleccionado
            @NotNull(message = "El producto es obligatorio") Integer idProducto,
            // Cantidad obligatoria de unidades; como mínimo debe ser 1
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad minima es 1") Integer cantidad
    ) {
    }
}
