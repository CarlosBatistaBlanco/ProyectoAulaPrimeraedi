package com.tecnicsecure.repository;

import com.tecnicsecure.entity.LogActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LogActividadRepository extends JpaRepository<LogActividad, Integer> {

    Page<LogActividad> findAllByOrderByCreadoEnDesc(Pageable pageable);

    @Query("""
        SELECT l FROM LogActividad l
        WHERE (:accion = '' OR LOWER(l.accion) = LOWER(:accion))
          AND (:texto = '' OR LOWER(l.accion) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(l.detalle, '')) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND l.creadoEn >= :desde
          AND l.creadoEn <= :hasta
        ORDER BY l.creadoEn DESC
    """)
    Page<LogActividad> buscarFiltrado(@Param("texto") String texto,
                                      @Param("accion") String accion,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      Pageable pageable);

    @Modifying
    int deleteByCreadoEnBefore(LocalDateTime limite);
}
