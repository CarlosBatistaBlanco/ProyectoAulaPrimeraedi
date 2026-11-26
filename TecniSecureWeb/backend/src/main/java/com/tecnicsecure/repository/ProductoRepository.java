/*
 * Este archivo sirve como el buscador que consulta, filtra y guarda los productos
 * del catálogo de la tienda. Se utiliza en la portada con artículos destacados, en la
 * barra de búsqueda del catálogo público y en las secciones de Productos e Inventario del panel.
 */
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

    // Encuentra un artículo específico usando su código de referencia comercial
    Optional<Producto> findBySku(String sku);

    // Trae los artículos visibles marcados como destacados para exhibirlos en la página principal
    List<Producto> findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc();

    // Busca por páginas artículos visibles por nombre o descripción, con filtros opcionales de categoría y marca
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

    // Trae los artículos visibles cuyas unidades disponibles llegaron al mínimo de alerta, del más escaso al mayor
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findProductosConStockBajo();

    // Cuenta cuántos artículos visibles tienen alerta por pocas unidades disponibles en bodega
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo")
    long countProductosConStockBajo();

    // Comprueba si ya existe un artículo guardado con ese código de referencia comercial
    boolean existsBySku(String sku);
}
