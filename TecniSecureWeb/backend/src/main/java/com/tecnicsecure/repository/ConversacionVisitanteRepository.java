/*
 * Este archivo sirve como el buscador que consulta y guarda las conversaciones del
 * asistente público con visitantes que no han iniciado sesión. Se utiliza en la burbuja
 * de chat del sitio web para retomar charlas y en la bandeja de Chat de Visitantes del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.ConversacionVisitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionVisitanteRepository extends JpaRepository<ConversacionVisitante, Integer> {

    // Encuentra una charla concreta usando el código privado que recuerda el navegador del visitante
    Optional<ConversacionVisitante> findByToken(String token);

    // Trae todas las conversaciones de visitantes ordenadas desde la que tuvo actividad más reciente
    List<ConversacionVisitante> findAllByOrderByActualizadoEnDesc();

    // Trae las conversaciones que están en un estado específico (como abiertas), de la más reciente a la más antigua
    List<ConversacionVisitante> findByEstadoOrderByActualizadoEnDesc(String estado);
}
