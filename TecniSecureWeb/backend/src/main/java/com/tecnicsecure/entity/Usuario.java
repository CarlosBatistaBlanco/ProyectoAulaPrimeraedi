/*
 * Este archivo define la ficha de cada cuenta de acceso a la plataforma (ya sea de
 * un administrador, un empleado o un cliente), con sus datos personales, clave protegida
 * y controles de seguridad. Se usa en el inicio de sesión, Mi Perfil y la sección de Usuarios del panel.
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
@Table(name = "usuarios")
public class Usuario {

    // Número único que identifica a cada cuenta y conexión con su tipo de perfil
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    // Nombre completo, correo de acceso, documento de identidad, teléfono y foto de perfil
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "documento", unique = true, length = 30)
    private String documento;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "avatar", length = 255)
    private String avatar;

    // Contraseña guardada de forma protegida e ilegible, más controles de cuenta activa o bloqueada
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "bloqueado")
    private Boolean bloqueado = false;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    // Fecha de la última vez que inició sesión y fecha en que se creó la cuenta
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Indica si esta cuenta corresponde a un administrador
    public boolean esAdministrador() {
        return rol != null && Rol.ADMINISTRADOR.equals(rol.getNombre());
    }

    // Indica si esta cuenta corresponde a un empleado
    public boolean esEmpleado() {
        return rol != null && Rol.EMPLEADO.equals(rol.getNombre());
    }

    // Indica si esta cuenta corresponde a un cliente
    public boolean esCliente() {
        return rol != null && Rol.CLIENTE.equals(rol.getNombre());
    }

    // Devuelve el nombre del perfil asignado a la cuenta, o vacío si aún no tiene uno
    public String getNombreRol() {
        return rol != null ? rol.getNombre() : null;
    }
}
