package com.tecnicsecure.repository;

import com.tecnicsecure.entity.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {

    List<MensajeChat> findByIdClienteOrderByCreadoEnAsc(Integer idCliente);

    List<MensajeChat> findTop100ByIdClienteOrderByCreadoEnDesc(Integer idCliente);

    long countByIdClienteAndRemitenteAndLeidoFalse(Integer idCliente, String remitente);
}
