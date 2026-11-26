/*
 * Este archivo define la ficha con la información laboral básica de cada integrante
 * del equipo de TecniServicios, como su cargo y la fecha en que ingresó. Esta información
 * se consulta y administra en la sección de Empleados y Usuarios del panel administrativo.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empleados")
public class Empleado {

    // Número único que identifica a cada integrante del equipo
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    // Conexión con la cuenta de acceso que usa este empleado
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    // Cargo que desempeña y fecha en que comenzó a trabajar en la empresa
    @Column(name = "cargo", length = 100)
    private String cargo = "Empleado";

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;
}
