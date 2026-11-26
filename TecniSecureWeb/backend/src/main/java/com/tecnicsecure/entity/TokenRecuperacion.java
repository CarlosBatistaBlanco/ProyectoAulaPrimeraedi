/*
 * Este archivo define la ficha del código temporal de un solo uso que el sistema
 * genera cuando una persona olvida su contraseña y solicita restablecerla. Se utiliza
 * en la pantalla de Recuperar Contraseña y al confirmar la nueva clave de acceso.
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
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    // Número único del registro y conexión con la persona que pidió cambiar su contraseña
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Integer idToken;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    // Código temporal secreto enviado por correo para confirmar la identidad de la persona
    @Column(name = "token", nullable = false, length = 100)
    private String token;

    // Fecha límite de validez del código, si ya fue utilizado y momento en que se creó
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado")
    private Boolean usado = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
