/*
 * Este archivo sirve como el buscador que guarda, consulta y elimina los mensajes
 * de las conversaciones del asistente público con visitantes sin cuenta. Se utiliza en
 * la burbuja de chat de la tienda y en la bandeja de Chat de Visitantes del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.MensajeVisitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeVisitanteRepository extends JpaRepository<MensajeVisitante, Integer> {

    // Trae todos los mensajes de una charla en orden, del primero al último
    List<MensajeVisitante> findByIdConversacionOrderByCreadoEnAsc(Integer idConversacion);

    // Cuenta cuántos mensajes aún no se han leído de un remitente concreto dentro de una charla
    long countByIdConversacionAndRemitenteAndLeidoFalse(Integer idConversacion, String remitente);

    // Trae los mensajes pendientes de lectura de un remitente dentro de una charla para poder marcarlos como leídos
    List<MensajeVisitante> findByIdConversacionAndRemitenteAndLeidoFalse(Integer idConversacion, String remitente);

    // Borra todos los mensajes que pertenecen a una conversación
    void deleteByIdConversacion(Integer idConversacion);
}
