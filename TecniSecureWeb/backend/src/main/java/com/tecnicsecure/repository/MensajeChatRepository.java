/*
 * Este archivo sirve como el buscador que guarda y consulta los mensajes intercambiados
 * en el chat entre clientes registrados y el equipo de atención. Se utiliza en la ventana
 * de Chat del cliente y en la bandeja de conversaciones del panel administrativo.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {

    // Trae toda la conversación de un cliente en orden, del mensaje más antiguo al más nuevo
    List<MensajeChat> findByIdClienteOrderByCreadoEnAsc(Integer idCliente);

    // Trae los 100 mensajes más recientes de un cliente, del último hacia atrás
    List<MensajeChat> findTop100ByIdClienteOrderByCreadoEnDesc(Integer idCliente);

    // Cuenta cuántos mensajes sin leer tiene un cliente enviados por una parte específica (cliente o asesor)
    long countByIdClienteAndRemitenteAndLeidoFalse(Integer idCliente, String remitente);
}
