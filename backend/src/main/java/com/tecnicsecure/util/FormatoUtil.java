package com.tecnicsecure.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades comunes (formato de dinero y normalizacion de texto). */
@Component
public class FormatoUtil {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    /** 1299900 -> "$1.299.900" */
    public String pesos(java.math.BigDecimal valor) {
        if (valor == null) {
            return "$0";
        }
        return String.format(COLOMBIA, "$%,.0f", valor.doubleValue()).replace(",", ".");
    }

    /** Comparacion sin acentos ni mayusculas (busquedas tolerantes). */
    public String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
