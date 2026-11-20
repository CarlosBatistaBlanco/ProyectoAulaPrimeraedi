package com.tecnicsecure.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La verificacion del login no puede depender de la sesion HTTP (la API es
 * STATELESS). Se usa una ficha firmada con HMAC que viaja con la pregunta.
 */
class CaptchaServiceTest {

    private final CaptchaService servicio = new CaptchaService("clave-de-prueba-para-firmar");

    /** Extrae la suma de "¿Cuánto es 5 + 8?" */
    private int sumaDe(String pregunta) {
        var m = java.util.regex.Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)").matcher(pregunta);
        assertThat(m.find()).as("la pregunta debe contener una suma").isTrue();
        return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
    }

    @Test
    @DisplayName("Acepta la suma correcta")
    void aceptaRespuestaCorrecta() {
        var desafio = servicio.generar();
        int suma = sumaDe(desafio.pregunta());
        assertThat(servicio.validar(desafio.ficha(), String.valueOf(suma))).isTrue();
    }

    @Test
    @DisplayName("Rechaza un numero equivocado (el bug reportado)")
    void rechazaNumeroEquivocado() {
        var desafio = servicio.generar();
        int suma = sumaDe(desafio.pregunta());
        int incorrecto = suma + 1;
        assertThat(servicio.validar(desafio.ficha(), String.valueOf(incorrecto))).isFalse();
    }

    @Test
    @DisplayName("Rechaza una ficha inventada por el cliente")
    void rechazaFichaFalsificada() {
        assertThat(servicio.validar("ZmFsc28uOTk5OTk5OTk5OS5maXJtYQ", "13")).isFalse();
        assertThat(servicio.validar("", "13")).isFalse();
        assertThat(servicio.validar(null, "13")).isFalse();
    }

    @Test
    @DisplayName("Rechaza el texto que no es un numero")
    void rechazaTexto() {
        var desafio = servicio.generar();
        assertThat(servicio.validar(desafio.ficha(), "abc")).isFalse();
        assertThat(servicio.validar(desafio.ficha(), "")).isFalse();
    }

    @Test
    @DisplayName("No se puede reutilizar la respuesta de otra pregunta")
    void fichaDeOtraPreguntaNoSirve() {
        var uno = servicio.generar();
        var otro = servicio.generar();
        int sumaUno = sumaDe(uno.pregunta());
        // La suma de la primera pregunta no valida contra la ficha de la segunda
        // (salvo casualidad de sumas iguales, que se descarta repitiendo).
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

    @Test
    @DisplayName("La ficha no revela el resultado en claro")
    void fichaNoExponeLaRespuesta() {
        var desafio = servicio.generar();
        int suma = sumaDe(desafio.pregunta());
        assertThat(desafio.ficha()).doesNotContain(String.valueOf(suma));
        assertThat(desafio.pregunta()).contains("+");
    }

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
