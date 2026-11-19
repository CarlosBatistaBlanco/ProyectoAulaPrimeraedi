package com.tecnicsecure.util;

import java.util.regex.Pattern;

/**
 * Reglas de formato compartidas por los formularios del sitio y del panel.
 * Las mismas expresiones estan replicadas en frontend/js/validaciones.js
 * para que el navegador avise antes de enviar; el backend siempre revalida.
 */
public final class PatronesValidacion {

    /** Solo digitos (documento de identidad). */
    public static final String DOCUMENTO = "^[0-9]{5,15}$";

    /** Telefono colombiano: 7 a 10 digitos, admite +57 y separadores. */
    public static final String TELEFONO = "^(\\+?57)?[\\s-]?(\\(?\\d{1,3}\\)?[\\s-]?)?\\d{7,10}$";

    /** Correo con dominio completo (exige punto y extension de 2+ letras). */
    public static final String CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";

    /** Contrasena: minimo 8, una mayuscula, una minuscula y un numero. */
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    /** Solo letras, espacios y acentos (nombres y apellidos). */
    public static final String NOMBRE = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü' .-]{2,119}$";

    private static final Pattern P_DOCUMENTO = Pattern.compile(DOCUMENTO);
    private static final Pattern P_TELEFONO = Pattern.compile(TELEFONO);
    private static final Pattern P_CORREO = Pattern.compile(CORREO);

    private PatronesValidacion() {
    }

    /** Util para validar en los servicios cuando el dato llega "a mano". */
    public static boolean documentoValido(String valor) {
        return valor != null && P_DOCUMENTO.matcher(valor.trim()).matches();
    }

    public static boolean telefonoValido(String valor) {
        return valor != null && P_TELEFONO.matcher(valor.trim()).matches();
    }

    public static boolean correoValido(String valor) {
        return valor != null && P_CORREO.matcher(valor.trim()).matches();
    }

    /** Deja el telefono solo con digitos y el prefijo +57 si lo traia. */
    public static String normalizarTelefono(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replaceAll("[^0-9+]", "");
        return limpio.isEmpty() ? null : limpio;
    }
}
