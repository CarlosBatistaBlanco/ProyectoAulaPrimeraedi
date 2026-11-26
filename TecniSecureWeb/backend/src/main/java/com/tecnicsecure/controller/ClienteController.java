/*
 * Este archivo atiende las acciones personales del cliente en el catálogo y en la sección
 * Mis Favoritos de su panel. Permite guardar o quitar productos con el ícono de corazón,
 * consultar la lista de favoritos y leer o publicar calificaciones con estrellas y opiniones.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.entity.Resena;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.FavoritoRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.repository.ResenaRepository;
import com.tecnicsecure.util.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "7. Cliente", description = "Favoritos y resenas de productos")
@RestController
@RequestMapping("/api/cliente")
public class ClienteController {

    private final FavoritoRepository favoritoRepository;
    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioActual usuarioActual;

    // Conecta con los registros de favoritos, reseñas, productos y los datos de quien tiene la sesión abierta
    public ClienteController(FavoritoRepository favoritoRepository,
                             ResenaRepository resenaRepository,
                             ProductoRepository productoRepository,
                             UsuarioActual usuarioActual) {
        this.favoritoRepository = favoritoRepository;
        this.resenaRepository = resenaRepository;
        this.productoRepository = productoRepository;
        this.usuarioActual = usuarioActual;
    }

    // Datos que envía el cliente al calificar un producto: puntaje de 1 a 5 estrellas y comentario opcional
    public record ResenaRequest(
            @NotNull @Min(1) @Max(5) Integer calificacion,
            String comentario) {
    }

    // Entrega los productos marcados con corazón para mostrarlos en la sección Mis Favoritos
    @Operation(summary = "Mis productos favoritos")
    @GetMapping("/favoritos")
    public List<Map<String, Object>> favoritos() {
        Integer idUsuario = usuarioActual.requerir().getIdUsuario();
        // Busca cada producto guardado por el cliente y arma una tarjeta sencilla con nombre, precio y foto
        return favoritoRepository.findByIdUsuario(idUsuario).stream()
                .map(f -> productoRepository.findById(f.getIdProducto()))
                .flatMap(java.util.Optional::stream)
                .map(p -> Map.<String, Object>of(
                        "idProducto", p.getIdProducto(),
                        "nombre", p.getNombre(),
                        "precio", p.getPrecio(),
                        "precioFinal", p.getPrecioFinal(),
                        "imagen", p.getImagen() == null ? "" : p.getImagen()))
                .toList();
    }

    // Marca un producto como favorito cuando el cliente pulsa el corazón, o lo quita si ya lo tenía guardado
    @Operation(summary = "Agregar o quitar un producto de favoritos")
    @PostMapping("/favoritos/{idProducto}")
    @Transactional
    public Map<String, Object> alternarFavorito(@PathVariable Integer idProducto) {
        Integer idUsuario = usuarioActual.requerir().getIdUsuario();
        // Confirma primero que el producto exista en la tienda
        productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        // Si ya estaba en la lista de favoritos lo quita; si no estaba, lo agrega
        var existente = favoritoRepository.findByIdUsuarioAndIdProducto(idUsuario, idProducto);
        if (existente.isPresent()) {
            favoritoRepository.delete(existente.get());
            return Map.of("favorito", false);
        }

        var favorito = new com.tecnicsecure.entity.Favorito();
        favorito.setIdUsuario(idUsuario);
        favorito.setIdProducto(idProducto);
        favoritoRepository.save(favorito);
        return Map.of("favorito", true);
    }

    // Muestra todas las opiniones y estrellas recibidas por un producto, empezando por la más nueva
    @Operation(summary = "Resenas de un producto")
    @GetMapping("/productos/{idProducto}/resenas")
    public List<Resena> resenas(@PathVariable Integer idProducto) {
        return resenaRepository.findByIdProductoOrderByCreadoEnDesc(idProducto);
    }

    // Publica la calificación y opinión del cliente sobre un producto (máximo una reseña por cliente)
    @Operation(summary = "Resenar un producto (una vez por usuario)")
    @PostMapping("/productos/{idProducto}/resenas")
    public ResponseEntity<Resena> resenar(@PathVariable Integer idProducto,
                                          @Valid @RequestBody ResenaRequest request) {
        Integer idUsuario = usuarioActual.requerir().getIdUsuario();
        // Revisa que el producto exista y que esta persona no lo haya calificado antes
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        if (resenaRepository.findByIdProductoAndIdUsuario(idProducto, idUsuario).isPresent()) {
            throw ApiException.conflict("Ya calificaste este producto");
        }

        // Guarda la calificación y el comentario del cliente en el sistema
        Resena resena = new Resena();
        resena.setIdProducto(producto.getIdProducto());
        resena.setIdUsuario(idUsuario);
        resena.setCalificacion(request.calificacion());
        resena.setComentario(request.comentario());
        resenaRepository.save(resena);
        return ResponseEntity.status(HttpStatus.CREATED).body(resena);
    }
}
