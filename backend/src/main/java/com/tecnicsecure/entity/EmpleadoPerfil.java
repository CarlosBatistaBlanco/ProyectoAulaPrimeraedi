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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil_empleado")
    private Integer idPerfilEmpleado;

    @Column(name = "id_usuario", nullable = false, unique = true)
    private Integer idUsuario;

    @Column(name = "area", nullable = false, length = 40)
    private String area = "OPERATIVO";

    @Column(name = "modulos_permitidos", nullable = false, length = 300)
    private String modulosPermitidos = "dashboard,tareas,perfil,direccion,ventas,pedidos,facturacion,inventario,productos,soporte,chat";

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
