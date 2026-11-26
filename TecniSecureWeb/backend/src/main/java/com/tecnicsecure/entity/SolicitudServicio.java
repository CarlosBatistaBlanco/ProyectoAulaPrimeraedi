/*
 * Este archivo define la ficha de cada petición de cotización o visita técnica que
 * envían los interesados, junto con la fecha deseada, el técnico asignado y su estado.
 * Se utiliza en el formulario de Servicios del sitio público y en la sección de Solicitudes del panel.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitudes_servicio")
public class SolicitudServicio {

    // Número único de la solicitud y conexión con el servicio técnico elegido
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Datos de contacto y domicilio de la persona que solicita la atención
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "direccion", length = 255)
    private String direccion;

    // Modalidad pedida (cotización o visita), fecha y hora preferidas y explicación de lo que necesita
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "cotizacion";

    @Column(name = "fecha_preferida")
    private LocalDate fechaPreferida;

    @Column(name = "hora_preferida")
    private LocalTime horaPreferida;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Seguimiento del equipo: técnico asignado, notas internas, estado actual y fecha de envío
    @Column(name = "tecnico_asignado", length = 120)
    private String tecnicoAsignado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "estado", length = 20)
    private String estado = "pendiente";

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
