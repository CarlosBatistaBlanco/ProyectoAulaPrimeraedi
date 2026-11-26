/*
 * Este archivo sirve como el buscador que consulta y guarda todas las cuentas de
 * acceso a la plataforma (administradores, empleados y clientes). Se utiliza al iniciar
 * sesión, al registrarse, en Mi Perfil y al buscar o administrar cuentas en Usuarios del panel.
 */
package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Encuentra una cuenta por su correo electrónico sin distinguir mayúsculas o minúsculas
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    // Comprueba si ese correo electrónico ya está siendo usado por otra cuenta
    boolean existsByCorreoIgnoreCase(String correo);

    // Comprueba si ese número de documento ya está registrado en otra cuenta
    boolean existsByDocumento(String documento);

    // Trae todas las personas que tienen un perfil específico (por ejemplo, administradores o empleados)
    List<Usuario> findByRolNombre(String nombreRol);

    // Cuenta cuántas cuentas se encuentran activas actualmente en la plataforma
    long countByActivoTrue();

    // Busca cuentas por páginas para no cargar todo a la vez, filtrando por nombre o correo y opcionalmente por perfil
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:texto IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                 OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:rol IS NULL OR u.rol.nombre = :rol)
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("rol") String rol, Pageable pageable);
}
