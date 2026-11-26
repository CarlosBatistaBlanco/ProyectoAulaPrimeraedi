/*
 * Este archivo define los tipos de perfil o niveles de acceso que existen en la
 * plataforma: administrador, empleado y cliente. Esta información se utiliza al iniciar
 * sesión, al decidir qué pantallas puede ver cada persona y en la gestión de Usuarios.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Rol {

    // Nombres fijos de los tres tipos de perfil que maneja la plataforma
    public static final String ADMINISTRADOR = "administrador";
    public static final String EMPLEADO = "empleado";
    public static final String CLIENTE = "cliente";

    // Número único que identifica a cada tipo de perfil
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    // Nombre irrepetible del perfil guardado en el sistema
    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;
}
