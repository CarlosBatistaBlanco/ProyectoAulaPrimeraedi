/*
 * Este archivo prepara la información de cada servicio técnico ofrecido por
 * TecniServicios (como instalación, soporte o mantenimiento) para mostrarla en la
 * sección de servicios de la página principal y en el catálogo público.
 */
package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Servicio;

public record ServicioResponse(
        // Número único que identifica el servicio
        Integer idServicio,
        // Nombre comercial del servicio que verá el cliente
        String titulo,
        // Explicación breve de lo que incluye este servicio
        String descripcion,
        // Ícono representativo que acompaña la tarjeta en pantalla
        String icono,
        // Número que define el orden en el que se muestra el servicio en la página
        Integer orden
) {
    // Toma un servicio guardado y prepara sus datos listos para presentarlos en la página
    public static ServicioResponse de(Servicio s) {
        return new ServicioResponse(s.getIdServicio(), s.getTitulo(), s.getDescripcion(),
                s.getIcono(), s.getOrden());
    }
}
