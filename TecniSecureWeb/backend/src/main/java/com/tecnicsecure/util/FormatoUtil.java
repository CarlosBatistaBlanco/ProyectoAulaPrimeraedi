/*
 * Este archivo ofrece ayudas rápidas de presentación y búsqueda que usan distintas pantallas
 * de la tienda. Da formato a los precios en pesos colombianos (por ejemplo $1.299.900) y
 * limpia tildes y mayúsculas para que el buscador encuentre productos sin importar cómo se escriban.
 */
package com.tecnicsecure.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class FormatoUtil {

    // Idioma y país de Colombia para mostrar los precios con el formato habitual de pesos
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    // Convierte un número de precio (como 1299900) a formato legible de pesos colombianos ("$1.299.900")
    public String pesos(java.math.BigDecimal valor) {
        if (valor == null) {
            return "$0";
        }
        return String.format(COLOMBIA, "$%,.0f", valor.doubleValue()).replace(",", ".");
    }

    // Quita tildes, pasa a minúsculas y borra espacios sobrantes para buscar palabras sin fallar por acentos
    public String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        // Separa cada letra de su tilde y elimina las marcas de acento antes de comparar
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
