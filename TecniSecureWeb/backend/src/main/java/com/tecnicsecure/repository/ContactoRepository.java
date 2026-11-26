/*
 * Este archivo sirve como el buscador que guarda y consulta los mensajes enviados
 * desde el formulario de contacto del sitio web. Se utiliza cuando una persona escribe
 * una consulta y cuando el equipo atiende o cuenta los mensajes pendientes en Soporte del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Contacto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Integer> {

    // Busca por páginas los mensajes aún sin leer, del más reciente al más antiguo, para no cargar todo a la vez
    Page<Contacto> findByLeidoFalseOrderByCreadoEnDesc(Pageable pageable);

    // Cuenta cuántos mensajes enviados siguen pendientes por revisar
    long countByLeidoFalse();
}
