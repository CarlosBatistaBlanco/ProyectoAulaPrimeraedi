package com.tecnicsecure.service;

import com.tecnicsecure.dto.direccion.DireccionRequest;
import com.tecnicsecure.dto.direccion.DireccionResponse;

import java.util.List;

/**
 * Direcciones de entrega de cada usuario (menu "Direccion" en los tres roles).
 * Se usan para precargar el checkout cuando el usuario va a pagar.
 */
public interface DireccionService {

    List<DireccionResponse> misDirecciones();

    DireccionResponse crear(DireccionRequest request);

    DireccionResponse actualizar(Integer idDireccion, DireccionRequest request);

    void eliminar(Integer idDireccion);

    DireccionResponse marcarPredeterminada(Integer idDireccion);

    /** La predeterminada o, si no hay, la primera; null si no tiene ninguna. */
    DireccionResponse direccionPrincipal();
}
