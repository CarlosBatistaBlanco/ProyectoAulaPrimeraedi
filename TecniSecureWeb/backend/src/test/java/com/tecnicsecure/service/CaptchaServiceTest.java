/*
 * Este archivo contiene pruebas automáticas para la suma de seguridad que protege
 * la pantalla de inicio de sesión contra accesos automatizados. Comprueba que solo
 * deje pasar la respuesta correcta y rechace códigos falsificados o respuestas ajenas.
 */
package com.tecnicsecure.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaServiceTest {

    private final CaptchaService servicio = new CaptchaService("clave-de-prueba-para-firmar");

    // Lee los dos números de la pregunta generada (por ejemplo "¿Cuánto es 5 + 8?") y calcula el resultado real
    private int sumaDe(String pregunta) {
        var m = java.util.regex.Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)").matcher(pregunta);
        assertThat(m.find()).as("la pregunta debe contener una suma").isTrue();
        return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
    }

    // Comprueba que cuando la persona escribe el resultado correcto de la suma, la verificación lo acepte
    @Test
    @DisplayName("Acepta la suma correcta")
    void aceptaRespuestaCorrecta() {
        var desafio = servicio.generar();
        int suma = sumaDe(desafio.pregunta());
        assertThat(servicio.validar(desafio.ficha(), String.valueOf(suma))).isTrue();
    }

    // Comprueba que si la persona escribe un número equivocado, la verificación lo rechace
    @Test
    @DisplayName("Rechaza un numero equivocado (el bug reportado)")
    void rechazaNumeroEquivocado() {
        var desafio = servicio.generar();
        int suma = sumaDe(desafio.pregunta());
        int incorrecto = suma + 1;
        assertThat(servicio.validar(desafio.ficha(), String.valueOf(incorrecto))).isFalse();
    }

    // Comprueba que no se acepten comprobantes inventados, vacíos o ausentes
    @Test
    @DisplayName("Rechaza una ficha inventada por el cliente")
    void rechazaFichaFalsificada() {
        assertThat(servicio.validar("ZmFsc28uOTk5OTk5OTk5OS5maXJtYQ", "13")).isFalse();
        assertThat(servicio.validar("", "13")).isFalse();
        assertThat(servicio.validar(null, "13")).isFalse();
    }

    // Comprueba que escribir letras o dejar la casilla vacía no supere la verificación
    @Test
    @DisplayName("Rechaza el texto que no es un numero")
    void rechazaTexto() {
        var desafio = servicio.generar();
        assertThat(servicio.validar(desafio.ficha(), "abc")).isFalse();
        assertThat(servicio.validar(desafio.ficha(), "")).isFalse();
    }

    // Comprueba que la respuesta de una pregunta no pueda reutilizarse para responder otra pregunta distinta
    @Test
    @DisplayName("No se puede reutilizar la respuesta de otra pregunta")
    void fichaDeOtraPreguntaNoSirve() {
        var uno = servicio.generar();
        var otro = servicio.generar();
        int sumaUno = sumaDe(uno.pregunta());
        // Se generan varios pares de preguntas para asegurarse de probar dos sumas con resultados distintos
        boolean algunFalso = false;
        for (int i = 0; i < 20; i++) {
            var a = servicio.generar();
            var b = servicio.generar();
            int sumaA = sumaDe(a.pregunta());
            if (sumaA != sumaDe(b.pregunta()) && !servicio.validar(b.ficha(), String.valueOf(sumaA))) {
                algunFalso = true;
                break;
            }
        }
        assertThat(algunFalso).isTrue();
        assertThat(servicio.validar(uno.ficha(), String.valueOf(sumaUno))).isTrue();
        assertThat(servicio.validar(otro.ficha(), String.valueOf(sumaUno + 99))).isFalse();
    }

    // Comprueba que el comprobante enviado a la pantalla no deje ver a simple vista la respuesta correcta
    @Test
    @DisplayName("La ficha no revela el resultado en claro")
    void fichaNoExponeLaRespuesta() {
        // Se revisan varias preguntas seguidas para confirmar que el número ganador nunca viaje visible
        for (int i = 0; i < 15; i++) {
            var desafio = servicio.generar();
            int suma = sumaDe(desafio.pregunta());
            // El resultado siempre viaja protegido y nunca aparece escrito directamente en el comprobante
            assertThat(desafio.ficha()).doesNotContain("." + suma + ".");
            assertThat(desafio.pregunta()).contains("+").doesNotContain("=");
        }
    }

    // Comprueba que un comprobante creado con una clave secreta no sea aceptado si se valida con otra clave distinta
    @Test
    @DisplayName("Dos servidores con claves distintas no se aceptan entre si")
    void claveDistintaInvalidaLaFicha() {
        var emisor = new CaptchaService("clave-A");
        var validador = new CaptchaService("clave-B");
        var desafio = emisor.generar();
        int suma = sumaDe(desafio.pregunta());
        assertThat(validador.validar(desafio.ficha(), String.valueOf(suma))).isFalse();
    }
}
