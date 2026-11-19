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

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDocumento(String documento);

    List<Usuario> findByRolNombre(String nombreRol);

    long countByActivoTrue();

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:texto IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                 OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:rol IS NULL OR u.rol.nombre = :rol)
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("rol") String rol, Pageable pageable);
}
