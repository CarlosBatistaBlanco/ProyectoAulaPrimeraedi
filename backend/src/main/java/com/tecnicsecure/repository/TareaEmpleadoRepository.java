package com.tecnicsecure.repository;

import com.tecnicsecure.entity.TareaEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TareaEmpleadoRepository extends JpaRepository<TareaEmpleado, Integer> {

    List<TareaEmpleado> findByIdEmpleadoUsuarioOrderByCreadoEnDesc(Integer idEmpleadoUsuario);

    List<TareaEmpleado> findAllByOrderByCreadoEnDesc();

    long countByIdEmpleadoUsuarioAndEstadoIn(Integer idEmpleadoUsuario, Collection<String> estados);

    void deleteByIdEmpleadoUsuario(Integer idEmpleadoUsuario);
}
