package com.tecnicsecure.repository;

import com.tecnicsecure.entity.MensajeVisitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeVisitanteRepository extends JpaRepository<MensajeVisitante, Integer> {

    List<MensajeVisitante> findByIdConversacionOrderByCreadoEnAsc(Integer idConversacion);

    long countByIdConversacionAndRemitenteAndLeidoFalse(Integer idConversacion, String remitente);

    List<MensajeVisitante> findByIdConversacionAndRemitenteAndLeidoFalse(Integer idConversacion, String remitente);

    void deleteByIdConversacion(Integer idConversacion);
}
