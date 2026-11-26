/*
 * Este archivo recoge los datos que una persona llena en el formulario "Contáctanos"
 * de la página pública cuando quiere enviar una pregunta, un reclamo o una sugerencia.
 * Revisa que el nombre, el correo, el teléfono y el mensaje estén bien escritos.
 */
package com.tecnicsecure.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Mensaje del formulario de contacto")
public record ContactoRequest(
        // Nombre completo obligatorio de quien escribe (máximo 120 caracteres, solo letras y espacios)
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios") String nombre,
        // Correo electrónico obligatorio y con formato completo válido para poder responderle
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com") String correo,
        // Teléfono opcional de contacto; si se llena, debe tener entre 7 y 10 números
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)") String telefono,
        // Título o tema breve del mensaje (máximo 150 caracteres)
        @Size(max = 150) String asunto,
        // Clasificación permitida: mensaje, reclamo o sugerencia
        @Pattern(regexp = "mensaje|reclamo|sugerencia", message = "Tipo de mensaje no valido")
        String tipo,
        // Texto principal obligatorio del mensaje (máximo 4000 caracteres)
        @NotBlank(message = "El mensaje es obligatorio") @Size(max = 4000) String mensaje
) {
}
