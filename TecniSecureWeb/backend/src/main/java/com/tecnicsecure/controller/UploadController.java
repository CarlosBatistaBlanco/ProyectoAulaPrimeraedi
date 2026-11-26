/*
 * Este archivo atiende la subida de fotos y documentos desde los formularios de Mi Perfil,
 * Productos y Tareas del personal. Revisa que el tipo de archivo sea seguro, le asigna un
 * nombre único para no pisar otros archivos y lo guarda en la carpeta de subidas del servidor.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Tag(name = "10. Archivos", description = "Carga real de imagenes y archivos de evidencia para tareas y catalogo")
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final com.tecnicsecure.config.TecniSecureProperties propiedades;

    // Recibe los ajustes generales de la tienda para saber en qué carpeta guardar lo que se suba
    public UploadController(com.tecnicsecure.config.TecniSecureProperties propiedades) {
        this.propiedades = propiedades;
    }

    // Ubica la misma carpeta de archivos subidos que luego se muestra públicamente en la tienda
    private Path carpetaUploads() {
        // Toma la carpeta definida en los ajustes; si no hay una, usa la carpeta uploads de la página
        String configurada = propiedades.uploads() != null ? propiedades.uploads().dir() : null;
        if (configurada != null && !configurada.isBlank()) {
            return Paths.get(configurada).toAbsolutePath().normalize();
        }
        String frontend = System.getProperty("frontend.dir");
        Path raiz = frontend != null && !frontend.isBlank()
                ? Paths.get(frontend)
                : Paths.get("..", "frontend");
        return raiz.toAbsolutePath().normalize().resolve("uploads");
    }

    // Tipos de imagen aceptados para fotos de catálogo y fotos de perfil
    private static final List<String> EXTENSIONES_IMAGEN = List.of("jpg", "jpeg", "png", "webp", "gif", "svg");
    // Tipos de documentos e imágenes aceptados como comprobante de una tarea terminada
    private static final List<String> EXTENSIONES_EVIDENCIA = List.of(
            "jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx", "xls", "xlsx", "txt", "zip"
    );

    // Recibe y guarda una foto de producto o una foto de perfil (requiere haber iniciado sesión)
    @Operation(summary = "Subir imagen (producto o avatar)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/imagen")
    public Map<String, String> subirImagen(@RequestParam("archivo") MultipartFile archivo,
                                           @RequestParam(defaultValue = "productos") String tipo) throws IOException {
        String carpeta = "avatars".equalsIgnoreCase(tipo) ? "avatars" : "productos";
        return guardarArchivo(archivo, carpeta, EXTENSIONES_IMAGEN, "Usa JPG, PNG, WEBP, GIF o SVG");
    }

    // Recibe y guarda un documento o imagen como prueba de avance de una tarea (solo empleados o administradores)
    @Operation(summary = "Subir archivo real de evidencia para tarea (PDF, imagen, Office, TXT o ZIP)")
    @PreAuthorize("hasAnyRole('EMPLEADO','ADMINISTRADOR')")
    @PostMapping("/evidencia")
    public Map<String, String> subirEvidencia(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return guardarArchivo(archivo, "evidencias", EXTENSIONES_EVIDENCIA,
                "Usa PDF, imagen (JPG/PNG/WEBP), Word, Excel, TXT o ZIP");
    }

    // Revisa que el archivo no venga vacío ni sea de un tipo prohibido, le da un nombre único y lo guarda
    private Map<String, String> guardarArchivo(MultipartFile archivo,
                                             String carpeta,
                                             List<String> permitidas,
                                             String mensajeFormato) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.badRequest("Debes seleccionar un archivo");
        }
        // Revisa la terminación del nombre del archivo para confirmar que sea un formato seguro
        String original = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo.bin";
        int idx = original.lastIndexOf('.');
        String ext = idx >= 0 ? original.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
        if (!permitidas.contains(ext)) {
            throw ApiException.badRequest("Formato no permitido. " + mensajeFormato);
        }

        // Crea la subcarpeta si aún no existe y guarda el archivo con un nombre aleatorio para evitar reemplazos
        Path base = carpetaUploads().resolve(carpeta);
        Files.createDirectories(base);

        String nombreArchivo = UUID.randomUUID().toString().substring(0, 12) + "." + ext;
        Path destino = base.resolve(nombreArchivo);
        Files.write(destino, archivo.getBytes());

        // Entrega la dirección web donde quedará visible la foto o descargable el documento
        String rutaRelativa = "uploads/" + carpeta + "/" + nombreArchivo;
        return Map.of(
                "ruta", rutaRelativa,
                "url", "/" + rutaRelativa,
                "nombreOriginal", original
        );
    }
}
