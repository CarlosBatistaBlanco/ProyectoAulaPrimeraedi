package com.tecnicsecure.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Solicitud de cotizacion o cita tecnica")
public record SolicitudRequest(
        @NotNull(message = "El servicio es obligatorio") Integer idServicio,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios") String nombre,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com") String correo,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)") String telefono,
        @Size(max = 255) String direccion,
        @NotBlank(message = "El tipo es obligatorio (cotizacion o cita)") String tipo,
        @jakarta.validation.constraints.FutureOrPresent(message = "La fecha no puede estar en el pasado")
        LocalDate fechaPreferida,
        LocalTime horaPreferida,
        @Size(max = 2000) String descripcion
) {
}
