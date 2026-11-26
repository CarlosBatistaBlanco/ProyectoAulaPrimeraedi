/*
 * Este archivo agrupa la información que llena una persona en el formulario de
 * cotización o agendamiento de cita técnica dentro de la web. Comprueba que sus
 * datos de contacto estén completos y que la fecha elegida no sea del pasado.
 */
package com.tecnicsecure.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Solicitud de cotizacion o cita tecnica")
public record SolicitudRequest(
        // Identificador obligatorio del servicio técnico que se está solicitando
        @NotNull(message = "El servicio es obligatorio") Integer idServicio,
        // Nombre completo obligatorio del solicitante (máximo 120 caracteres, solo letras y espacios)
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios") String nombre,
        // Correo electrónico obligatorio y con formato completo válido para enviar confirmaciones
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com") String correo,
        // Teléfono obligatorio de contacto; solo números y entre 7 y 10 dígitos
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)") String telefono,
        // Dirección opcional donde se realizaría la visita técnica (máximo 255 caracteres)
        @Size(max = 255) String direccion,
        // Tipo de solicitud obligatorio: cotización o cita
        @NotBlank(message = "El tipo es obligatorio (cotizacion o cita)") String tipo,
        // Fecha deseada para la atención (solo permite hoy o fechas futuras) y hora preferida
        @jakarta.validation.constraints.FutureOrPresent(message = "La fecha no puede estar en el pasado")
        LocalDate fechaPreferida,
        LocalTime horaPreferida,
        // Explicación adicional del problema o de lo que necesita el cliente (máximo 2000 caracteres)
        @Size(max = 2000) String descripcion
) {
}
