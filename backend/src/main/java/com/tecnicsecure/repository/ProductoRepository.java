package com.tecnicsecure.repository;

import com.tecnicsecure.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findBySku(String sku);

    List<Producto> findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc();

    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = true
              AND (:texto IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(COALESCE(p.descripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:idCategoria IS NULL OR p.categoria.idCategoria = :idCategoria)
              AND (:idMarca IS NULL OR p.marca.idMarca = :idMarca)
            """)
    Page<Producto> buscar(@Param("texto") String texto,
                          @Param("idCategoria") Integer idCategoria,
                          @Param("idMarca") Integer idMarca,
                          Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findProductosConStockBajo();

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo")
    long countProductosConStockBajo();

    boolean existsBySku(String sku);
}
