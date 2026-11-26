/*
 * Este archivo define la ficha que guarda los mensajes, preguntas o solicitudes de
 * ayuda que envían las personas desde el formulario web, junto con la respuesta
 * del equipo. Se utiliza en la página de atención al cliente del sitio y en la bandeja de Soporte del panel.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contactos")
public class Contacto {

    // Número único que identifica cada mensaje recibido
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contacto")
    private Integer idContacto;

    // Datos de contacto de la persona que escribió
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    // Motivo de la consulta, texto enviado y respuesta del equipo
    @Column(name = "asunto", length = 150)
    private String asunto;

    @Column(name = "tipo", length = 20)
    private String tipo = "mensaje";

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    // Conexión con la cuenta del remitente, si escribió con su sesión iniciada
    @Column(name = "id_usuario")
    private Integer idUsuario;

    // Indica si el equipo ya revisó el mensaje y cuándo fue enviado
    @Column(name = "leido")
    private Boolean leido = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
