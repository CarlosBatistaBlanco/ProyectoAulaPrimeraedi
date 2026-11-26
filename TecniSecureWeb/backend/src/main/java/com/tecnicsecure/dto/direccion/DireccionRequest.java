/*
 * Este archivo reúne los datos que un cliente escribe cuando guarda o actualiza una
 * dirección de entrega en su libreta personal (como "Casa" u "Oficina"). Se usa en la
 * pantalla de direcciones del perfil y al registrar un lugar nuevo durante una compra.
 */
package com.tecnicsecure.dto.direccion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Direccion de entrega guardada por el usuario")
public record DireccionRequest(
        // Nombre corto obligatorio para identificar el lugar, por ejemplo "Casa" u "Oficina" (máximo 60 caracteres)
        @Schema(example = "Casa")
        @NotBlank(message = "Ponle un nombre para reconocerla (Casa, Oficina...)")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String etiqueta,

        // Dirección física obligatoria; debe tener entre 8 y 255 caracteres
        @Schema(example = "Calle 45 # 12-34, Bogotá")
        @NotBlank(message = "La direccion es obligatoria")
        @Size(min = 8, max = 255, message = "La direccion debe tener entre 8 y 255 caracteres")
        String direccion,

        // Ciudad y barrio opcionales (máximo 100 caracteres cada uno)
        @Size(max = 100) String ciudad,
        @Size(max = 100) String barrio,

        // Teléfono de contacto para coordinar la entrega; solo números entre 7 y 10 dígitos
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos")
        String telefono,

        // Nombre de la persona que recibe el pedido e indicaciones adicionales para ubicar el lugar
        @Size(max = 120) String nombreRecibe,
        @Size(max = 255) String referencia,

        // Ubicación norte-sur seleccionada en el mapa (debe estar dentro del rango válido de -90 a 90)
        @Schema(description = "Latitud entre -90 y 90")
        @DecimalMin(value = "-90.0", message = "Latitud fuera de rango")
        @DecimalMax(value = "90.0", message = "Latitud fuera de rango")
        Double latitud,

        // Ubicación este-oeste seleccionada en el mapa (debe estar dentro del rango válido de -180 a 180)
        @Schema(description = "Longitud entre -180 y 180")
        @DecimalMin(value = "-180.0", message = "Longitud fuera de rango")
        @DecimalMax(value = "180.0", message = "Longitud fuera de rango")
        Double longitud,

        // Indica si esta dirección quedará marcada como la opción principal del cliente
        Boolean predeterminada
) {
}
