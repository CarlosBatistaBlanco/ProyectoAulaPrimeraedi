package com.tecnicsecure.service;

import com.tecnicsecure.dto.catalogo.*;
import com.tecnicsecure.dto.comun.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogoService {

    PageResponse<ProductoResumen> listar(String texto, Integer idCategoria, Integer idMarca, Pageable pageable);

    List<ProductoResumen> destacados();

    ProductoDetalle obtener(Integer idProducto);

    ProductoDetalle crear(ProductoRequest request);

    ProductoDetalle actualizar(Integer idProducto, ProductoRequest request);

    void eliminar(Integer idProducto);

    List<CategoriaRequest> categorias();

    List<MarcaRequest> marcas();

    List<ServicioResponse> servicios();

    Integer crearCategoria(CategoriaRequest request);

    Integer crearMarca(MarcaRequest request);
}
