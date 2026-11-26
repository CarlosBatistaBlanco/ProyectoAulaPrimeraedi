/*
 * Este archivo guarda las reglas oficiales que revisan que los datos escritos en los
 * formularios de registro, perfil, compras y usuarios estén bien escritos (documento,
 * teléfono colombiano, correo, contraseña segura y nombre). El servidor siempre vuelve
 * a aplicar estas reglas como respaldo aunque la pantalla ya haya revisado antes.
 */
package com.tecnicsecure.util;

import java.util.regex.Pattern;

public final class PatronesValidacion {

    // Regla para documento de identidad: únicamente números, entre 5 y 15 dígitos
    public static final String DOCUMENTO = "^[0-9]{5,15}$";

    // Regla para teléfono en Colombia: entre 7 y 10 dígitos, aceptando +57 y espacios o guiones
    public static final String TELEFONO = "^(\\+?57)?[\\s-]?(\\(?\\d{1,3}\\)?[\\s-]?)?\\d{7,10}$";

    // Regla para correo electrónico: usuario, arroba (@) y un dominio real con punto y extensión
    public static final String CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";

    // Regla para contraseña segura: mínimo 8 caracteres, una mayúscula, una minúscula y un número
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    // Regla para nombres y apellidos: solo letras, tildes, espacios y signos habituales de nombres
    public static final String NOMBRE = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü' .-]{2,119}$";

    // Moldes preparados de antemano para comprobar documentos, teléfonos y correos con rapidez
    private static final Pattern P_DOCUMENTO = Pattern.compile(DOCUMENTO);
    private static final Pattern P_TELEFONO = Pattern.compile(TELEFONO);
    private static final Pattern P_CORREO = Pattern.compile(CORREO);

    // Impide crear copias innecesarias porque este archivo solo guarda reglas y ayudas reutilizables
    private PatronesValidacion() {
    }

    // Comprueba que el documento de identidad tenga únicamente números entre 5 y 15 cifras
    public static boolean documentoValido(String valor) {
        return valor != null && P_DOCUMENTO.matcher(valor.trim()).matches();
    }

    // Comprueba que el número telefónico cumpla con un formato colombiano válido
    public static boolean telefonoValido(String valor) {
        return valor != null && P_TELEFONO.matcher(valor.trim()).matches();
    }

    // Comprueba que el correo electrónico tenga una forma válida con arroba y dominio completo
    public static boolean correoValido(String valor) {
        return valor != null && P_CORREO.matcher(valor.trim()).matches();
    }

    // Limpia espacios, guiones o paréntesis del teléfono dejando solo números y el prefijo +57 si lo tenía
    public static String normalizarTelefono(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replaceAll("[^0-9+]", "");
        return limpio.isEmpty() ? null : limpio;
    }
}
