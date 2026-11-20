package com.tecnicsecure.dto.direccion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Direccion de entrega guardada por el usuario")
public record DireccionRequest(
        @Schema(example = "Casa")
        @NotBlank(message = "Ponle un nombre para reconocerla (Casa, Oficina...)")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String etiqueta,

        @Schema(example = "Calle 45 # 12-34, Bogotá")
        @NotBlank(message = "La direccion es obligatoria")
        @Size(min = 8, max = 255, message = "La direccion debe tener entre 8 y 255 caracteres")
        String direccion,

        @Size(max = 100) String ciudad,
        @Size(max = 100) String barrio,

        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos")
        String telefono,

        @Size(max = 120) String nombreRecibe,
        @Size(max = 255) String referencia,

        @Schema(description = "Latitud entre -90 y 90")
        @DecimalMin(value = "-90.0", message = "Latitud fuera de rango")
        @DecimalMax(value = "90.0", message = "Latitud fuera de rango")
        Double latitud,

        @Schema(description = "Longitud entre -180 y 180")
        @DecimalMin(value = "-180.0", message = "Longitud fuera de rango")
        @DecimalMax(value = "180.0", message = "Longitud fuera de rango")
        Double longitud,

        Boolean predeterminada
) {
}
