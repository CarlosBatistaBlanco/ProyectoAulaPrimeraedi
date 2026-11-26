/*
 * Este archivo define la ficha que guarda el área de trabajo asignada a un empleado
 * y la lista de secciones del panel a las que tiene permiso de entrar. Se utiliza para
 * mostrarle solo las herramientas que le corresponden y administrarlo en Usuarios del panel.
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
@Table(name = "empleados_perfiles")
public class EmpleadoPerfil {

    // Número único del perfil y conexión con el empleado al que corresponde
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil_empleado")
    private Integer idPerfilEmpleado;

    @Column(name = "id_usuario", nullable = false, unique = true)
    private Integer idUsuario;

    // Área de trabajo asignada y lista de pantallas del panel que puede utilizar
    @Column(name = "area", nullable = false, length = 40)
    private String area = "OPERATIVO";

    @Column(name = "modulos_permitidos", nullable = false, length = 300)
    private String modulosPermitidos = "dashboard,tareas,perfil,direccion,ventas,pedidos,facturacion,inventario,productos,soporte,chat";

    // Última fecha y hora en que se ajustaron estos permisos
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
