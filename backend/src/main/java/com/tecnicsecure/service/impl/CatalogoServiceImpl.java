package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.catalogo.*;
import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.entity.Categoria;
import com.tecnicsecure.entity.Marca;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.entity.ProductoImagen;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.CatalogoService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final ResenaRepository resenaRepository;

    public CatalogoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               MarcaRepository marcaRepository,
                               ServicioRepository servicioRepository,
                               ProductoImagenRepository productoImagenRepository,
                               ResenaRepository resenaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.servicioRepository = servicioRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductoResumen> listar(String texto, Integer idCategoria, Integer idMarca,
                                                Pageable pageable) {
        String filtro = StringUtils.hasText(texto) ? texto.trim() : null;
        return PageResponse.de(productoRepository.buscar(filtro, idCategoria, idMarca, pageable),
                ProductoResumen::de);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumen> destacados() {
        return productoRepository.findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc()
                .stream()
                .limit(3)
                .map(ProductoResumen::de)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDetalle obtener(Integer idProducto) {
        Producto producto = buscar(idProducto);
        List<String> imagenes = productoImagenRepository
                .findByIdProductoOrderByOrdenAsc(idProducto)
                .stream()
                .map(ProductoImagen::getRuta)
                .toList();
        Double promedio = resenaRepository.promedioPorProducto(idProducto);
        Long total = resenaRepository.countByIdProducto(idProducto);
        return ProductoDetalle.de(producto, imagenes, promedio, total);
    }

    @Override
    @Transactional
    public ProductoDetalle crear(ProductoRequest request) {
        if (StringUtils.hasText(request.sku()) && productoRepository.existsBySku(request.sku().trim())) {
            throw ApiException.conflict("Ya existe un producto con el SKU " + request.sku());
        }
        Producto producto = new Producto();
        aplicarDatos(producto, request);
        productoRepository.save(producto);
        return obtener(producto.getIdProducto());
    }

    @Override
    @Transactional
    public ProductoDetalle actualizar(Integer idProducto, ProductoRequest request) {
        Producto producto = buscar(idProducto);
        if (StringUtils.hasText(request.sku())
                && !request.sku().trim().equalsIgnoreCase(producto.getSku())
                && productoRepository.existsBySku(request.sku().trim())) {
            throw ApiException.conflict("Ya existe un producto con el SKU " + request.sku());
        }
        aplicarDatos(producto, request);
        productoRepository.save(producto);
        return obtener(idProducto);
    }

    @Override
    @Transactional
    public void eliminar(Integer idProducto) {
        Producto producto = buscar(idProducto);
        // Baja logica: conserva el historial de pedidos y el kardex.
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaRequest> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaRequest(c.getNombre(), c.getIdCategoria()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarcaRequest> marcas() {
        return marcaRepository.findAll().stream()
                .map(m -> new MarcaRequest(m.getNombre(), m.getIdMarca()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioResponse> servicios() {
        return servicioRepository.findAllByOrderByOrdenAsc().stream()
                .map(ServicioResponse::de)
                .toList();
    }

    @Override
    @Transactional
    public Integer crearCategoria(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre().trim());
        return categoriaRepository.save(categoria).getIdCategoria();
    }

    @Override
    @Transactional
    public Integer crearMarca(MarcaRequest request) {
        String nombre = request.nombre().trim();
        if (marcaRepository.existsByNombre(nombre)) {
            throw ApiException.conflict("La marca ya existe");
        }
        Marca marca = new Marca();
        marca.setNombre(nombre);
        return marcaRepository.save(marca).getIdMarca();
    }

    private Producto buscar(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
    }

    private void aplicarDatos(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre().trim());
        producto.setSku(StringUtils.hasText(request.sku()) ? request.sku().trim() : null);
        producto.setCodigoBarras(StringUtils.hasText(request.codigoBarras()) ? request.codigoBarras().trim() : null);
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio() != null ? request.precio() : BigDecimal.ZERO);
        producto.setDescuentoPct(request.descuentoPct() != null ? request.descuentoPct() : BigDecimal.ZERO);
        if (request.impuestoPct() != null) {
            producto.setImpuestoPct(request.impuestoPct());
        }
        if (request.garantiaMeses() != null) {
            producto.setGarantiaMeses(request.garantiaMeses());
        }
        producto.setStock(request.stock() != null ? request.stock() : 0);
        if (request.stockMinimo() != null) {
            producto.setStockMinimo(request.stockMinimo());
        }
        if (request.stockMaximo() != null) {
            producto.setStockMaximo(request.stockMaximo());
        }
        if (StringUtils.hasText(request.imagen())) {
            producto.setImagen(request.imagen().trim());
        }
        producto.setDestacado(Boolean.TRUE.equals(request.destacado()));
        producto.setActivo(request.activo() == null || request.activo());

        producto.setCategoria(request.idCategoria() != null
                ? categoriaRepository.findById(request.idCategoria())
                    .orElseThrow(() -> ApiException.badRequest("Categoria no encontrada"))
                : null);
        producto.setMarca(request.idMarca() != null
                ? marcaRepository.findById(request.idMarca())
                    .orElseThrow(() -> ApiException.badRequest("Marca no encontrada"))
                : null);
    }
}
