package com.tecnicsecure.dto.pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Checkout del carrito de compras")
public record PedidoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios") String nombre,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com") String correo,
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)") String telefono,
        @Size(max = 255) String direccion,
        @NotEmpty(message = "El carrito esta vacio")
        @Valid
        List<ItemCarrito> carrito,
        @Size(max = 40) String metodoPago,
        @Size(max = 200) String detallePago,
        Double latitud,
        Double longitud
) {

    public PedidoRequest(String nombre, String correo, String telefono, String direccion, List<ItemCarrito> carrito) {
        this(nombre, correo, telefono, direccion, carrito, null, null, null, null);
    }

    @Schema(description = "Linea del carrito")
    public record ItemCarrito(
            @NotNull(message = "El producto es obligatorio") Integer idProducto,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad minima es 1") Integer cantidad
    ) {
    }
}
