package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    List<Servicio> findAllByOrderByOrdenAsc();
}
