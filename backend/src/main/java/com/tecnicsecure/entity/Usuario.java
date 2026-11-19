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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

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

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public boolean esAdministrador() {
        return rol != null && Rol.ADMINISTRADOR.equals(rol.getNombre());
    }

    public boolean esEmpleado() {
        return rol != null && Rol.EMPLEADO.equals(rol.getNombre());
    }

    public boolean esCliente() {
        return rol != null && Rol.CLIENTE.equals(rol.getNombre());
    }

    public String getNombreRol() {
        return rol != null ? rol.getNombre() : null;
    }
}
