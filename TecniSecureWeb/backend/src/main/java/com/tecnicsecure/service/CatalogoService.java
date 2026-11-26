/*
 * Este archivo define todas las operaciones para mostrar y administrar la vitrina de la tienda:
 * productos, categorias, marcas y servicios tecnicos. Su trabajo se ve en la pagina de inicio,
 * en el buscador de productos del sitio y en el panel donde el equipo agrega o edita equipos.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.catalogo.*;
import com.tecnicsecure.dto.comun.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogoService {

    // Busca productos por nombre, categoria o marca y los entrega divididos en paginas.
    PageResponse<ProductoResumen> listar(String texto, Integer idCategoria, Integer idMarca, Pageable pageable);

    // Trae los productos elegidos como destacados para lucirlos en la portada de la tienda.
    List<ProductoResumen> destacados();

    // Consulta la ficha completa de un producto con sus fotos, detalles y opiniones.
    ProductoDetalle obtener(Integer idProducto);

    // Crea un nuevo producto para publicarlo en la tienda.
    ProductoDetalle crear(ProductoRequest request);

    // Actualiza el nombre, precio, fotos o existencias de un producto ya registrado.
    ProductoDetalle actualizar(Integer idProducto, ProductoRequest request);

    // Oculta el producto de la tienda sin borrarlo para no danar facturas anteriores.
    void eliminar(Integer idProducto);

    // Entrega la lista completa de categorias para organizar y filtrar los productos.
    List<CategoriaRequest> categorias();

    // Entrega la lista completa de marcas disponibles en la tienda.
    List<MarcaRequest> marcas();

    // Muestra los servicios tecnicos que ofrece la empresa (instalacion, mantenimiento y mas).
    List<ServicioResponse> servicios();

    // Registra una categoria nueva para clasificar productos en la vitrina.
    Integer crearCategoria(CategoriaRequest request);

    // Registra una marca nueva para asignarla a los equipos del catalogo.
    Integer crearMarca(MarcaRequest request);
}
