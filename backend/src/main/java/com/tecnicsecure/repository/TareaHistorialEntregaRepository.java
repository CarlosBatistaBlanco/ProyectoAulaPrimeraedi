package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TareaHistorialEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TareaHistorialEntregaRepository extends JpaRepository<TareaHistorialEntrega, Integer> {

    List<TareaHistorialEntrega> findByIdTareaOrderByNumeroIntentoDesc(Integer idTarea);

    Optional<TareaHistorialEntrega> findFirstByIdTareaOrderByNumeroIntentoDesc(Integer idTarea);

    long countByIdTarea(Integer idTarea);
}
