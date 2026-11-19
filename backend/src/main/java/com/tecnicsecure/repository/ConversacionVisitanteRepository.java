package com.tecnicsecure.repository;

import com.tecnicsecure.entity.ConversacionVisitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionVisitanteRepository extends JpaRepository<ConversacionVisitante, Integer> {

    Optional<ConversacionVisitante> findByToken(String token);

    List<ConversacionVisitante> findAllByOrderByActualizadoEnDesc();

    List<ConversacionVisitante> findByEstadoOrderByActualizadoEnDesc(String estado);
}
