/*
 * Este archivo define las acciones para administrar la libreta de direcciones de entrega
 * de cada usuario. Su trabajo se nota en la seccion "Direccion" de los paneles y al pagar
 * un pedido, donde se rellenan automaticamente los datos de envio del cliente.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.direccion.DireccionRequest;
import com.tecnicsecure.dto.direccion.DireccionResponse;

import java.util.List;

public interface DireccionService {

    // Muestra todas las direcciones de entrega que guardo la persona conectada.
    List<DireccionResponse> misDirecciones();

    // Agrega una nueva direccion de entrega a la libreta del usuario actual.
    DireccionResponse crear(DireccionRequest request);

    // Edita los datos de una direccion ya guardada por el usuario.
    DireccionResponse actualizar(Integer idDireccion, DireccionRequest request);

    // Borra una direccion que el usuario ya no desea conservar.
    void eliminar(Integer idDireccion);

    // Elige cual direccion quedara marcada como favorita para los proximos pedidos.
    DireccionResponse marcarPredeterminada(Integer idDireccion);

    // Busca la direccion favorita del usuario (o la primera disponible) para rellenar el envio al comprar.
    DireccionResponse direccionPrincipal();
}
