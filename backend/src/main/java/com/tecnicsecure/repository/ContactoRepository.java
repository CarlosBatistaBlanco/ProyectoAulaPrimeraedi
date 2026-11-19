package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Contacto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Integer> {

    Page<Contacto> findByLeidoFalseOrderByCreadoEnDesc(Pageable pageable);

    long countByLeidoFalse();
}
