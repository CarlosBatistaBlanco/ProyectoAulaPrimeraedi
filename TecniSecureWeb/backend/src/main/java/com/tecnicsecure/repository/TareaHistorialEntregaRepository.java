/*
 * Este archivo sirve como el buscador que guarda y consulta cada intento de entrega
 * enviado por un empleado para una tarea. Se utiliza en la sección de Tareas del panel
 * para ver el historial completo de envíos y saber cuál es la última entrega por revisar.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TareaHistorialEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TareaHistorialEntregaRepository extends JpaRepository<TareaHistorialEntrega, Integer> {

    // Trae todos los intentos de entrega de una tarea, desde el más reciente hasta el primero
    List<TareaHistorialEntrega> findByIdTareaOrderByNumeroIntentoDesc(Integer idTarea);

    // Encuentra únicamente el intento de entrega más reciente registrado para una tarea
    Optional<TareaHistorialEntrega> findFirstByIdTareaOrderByNumeroIntentoDesc(Integer idTarea);

    // Cuenta cuántos intentos de entrega se han enviado para una tarea
    long countByIdTarea(Integer idTarea);
}
