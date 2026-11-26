/*
 * Este archivo ejecuta todas las reglas de la vitrina de la tienda: busca productos con filtros
 * tolerantes a tildes, mayusculas y plurales, arma las fichas detalladas con fotos y calificaciones,
 * guarda cambios de precios o descuentos y retira equipos sin borrar su historial.
 */
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final ResenaRepository resenaRepository;

    // Conecta el catalogo con los registros de productos, categorias, marcas, servicios, fotos y opiniones.
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

    // Busca productos filtrando por palabra (ignorando tildes y plurales), categoria o marca.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductoResumen> listar(String texto, Integer idCategoria, Integer idMarca,
                                                Pageable pageable) {
        if (!StringUtils.hasText(texto)) {
            return PageResponse.de(productoRepository.buscar(null, idCategoria, idMarca, pageable),
                    ProductoResumen::de);
        }

        // Obtiene los productos activos filtrados por categoria/marca y aplica busqueda sin tildes ni plurales
        Pageable todosOrdenados = PageRequest.of(0, 500, pageable.getSort());
        List<Producto> base = productoRepository.buscar(null, idCategoria, idMarca, todosOrdenados).getContent();

        String consultaNorm = normalizarTexto(texto);
        List<String> terminos = Arrays.stream(consultaNorm.split("[^a-z0-9]+"))
                .filter(t -> !t.isBlank())
                .map(this::raizSingular)
                .toList();

        List<Producto> filtrados = base.stream()
                .filter(p -> coincideProducto(p, consultaNorm, terminos))
                .toList();

        int inicio = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), filtrados.size());
        int fin = Math.min(inicio + pageable.getPageSize(), filtrados.size());
        List<Producto> sublista = filtrados.subList(inicio, fin);

        return PageResponse.de(new PageImpl<>(sublista, pageable, filtrados.size()), ProductoResumen::de);
    }

    // Comprueba si todos los terminos buscados aparecen en nombre, descripcion, SKU, marca o categoria sin importar tildes.
    private boolean coincideProducto(Producto p, String consultaCompleta, List<String> terminos) {
        String campos = normalizarTexto(
                (p.getNombre() != null ? p.getNombre() : "") + " "
                        + (p.getDescripcion() != null ? p.getDescripcion() : "") + " "
                        + (p.getSku() != null ? p.getSku() : "") + " "
                        + (p.getMarca() != null && p.getMarca().getNombre() != null ? p.getMarca().getNombre() : "") + " "
                        + (p.getCategoria() != null && p.getCategoria().getNombre() != null ? p.getCategoria().getNombre() : "")
        );
        if (campos.contains(consultaCompleta)) {
            return true;
        }
        if (terminos.isEmpty()) {
            return true;
        }
        for (String t : terminos) {
            if (!campos.contains(t)) {
                return false;
            }
        }
        return true;
    }

    // Quita tildes, dieresis y mayusculas para comparar palabras escritas de cualquier forma.
    private String normalizarTexto(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.replaceAll("\\s+", " ").trim();
    }

    // Convierte plurales comunes en espanol a su raiz singular (ej. "camaras" -> "camara", "sensores" -> "sensor").
    private String raizSingular(String palabra) {
        if (palabra.length() > 5 && palabra.endsWith("ores")) {
            return palabra.substring(0, palabra.length() - 2);
        }
        if (palabra.length() > 4 && palabra.endsWith("s") && !palabra.endsWith("ups")) {
            return palabra.substring(0, palabra.length() - 1);
        }
        return palabra;
    }

    // Trae hasta 3 productos activos marcados como destacados para exhibirlos en la portada de la tienda.
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumen> destacados() {
        return productoRepository.findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc()
                .stream()
                .limit(3)
                .map(ProductoResumen::de)
                .toList();
    }

    // Arma la ficha completa de un producto incluyendo su galeria de fotos, promedio de estrellas y cantidad de opiniones.
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

    // Crea un nuevo producto comprobando que su codigo de referencia no este repetido; si algo falla, no guarda datos incompletos.
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

    // Guarda cambios en un producto existente asegurandose de que su codigo de referencia no choque con otro equipo.
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

    // Oculta el producto de la tienda sin borrarlo de los registros para no danar facturas ni movimientos de bodega anteriores.
    @Override
    @Transactional
    public void eliminar(Integer idProducto) {
        Producto producto = buscar(idProducto);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    // Entrega todas las categorias registradas para llenar los filtros y formularios de la tienda.
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaRequest> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaRequest(c.getNombre(), c.getIdCategoria()))
                .toList();
    }

    // Entrega todas las marcas registradas para filtrar o clasificar los equipos.
    @Override
    @Transactional(readOnly = true)
    public List<MarcaRequest> marcas() {
        return marcaRepository.findAll().stream()
                .map(m -> new MarcaRequest(m.getNombre(), m.getIdMarca()))
                .toList();
    }

    // Trae los servicios tecnicos en el orden en que la empresa quiere presentarlos en el sitio web.
    @Override
    @Transactional(readOnly = true)
    public List<ServicioResponse> servicios() {
        return servicioRepository.findAllByOrderByOrdenAsc().stream()
                .map(ServicioResponse::de)
                .toList();
    }

    // Registra una nueva categoria para organizar los productos de la tienda.
    @Override
    @Transactional
    public Integer crearCategoria(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre().trim());
        return categoriaRepository.save(categoria).getIdCategoria();
    }

    // Registra una marca nueva verificando primero que no exista otra con exactamente el mismo nombre.
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

    // Busca un producto por su numero identificador y muestra un aviso claro si no existe.
    private Producto buscar(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
    }

    // Copia los datos llenados en el formulario al producto: precio, descuento, impuestos, garantia, existencias, categoria y marca.
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
