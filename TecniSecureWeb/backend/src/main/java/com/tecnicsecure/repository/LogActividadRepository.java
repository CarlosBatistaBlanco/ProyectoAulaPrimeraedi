/*
 * Este archivo sirve como el buscador que consulta, filtra y limpia la bitácora de
 * actividad del sistema. Se utiliza en la sección de Auditoría o Registro de Actividad
 * del panel administrativo para revisar qué acciones se hicieron y borrar registros antiguos.
 */
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

    // Busca los registros de actividad por páginas para no cargar todo a la vez, del más reciente al más antiguo
    Page<LogActividad> findAllByOrderByCreadoEnDesc(Pageable pageable);

    // Busca por páginas las acciones registradas filtrando por tipo de acción, palabra clave y rango de fechas
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

    // Borra las anotaciones de actividad anteriores a una fecha límite y devuelve cuántas se limpiaron
    @Modifying
    int deleteByCreadoEnBefore(LocalDateTime limite);
}
