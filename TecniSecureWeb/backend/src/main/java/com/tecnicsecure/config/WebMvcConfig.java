/*
 * Este archivo se encarga de entregar al navegador toda la parte visual de la tienda:
 * las pantallas, los colores, las imágenes, los videos y las fotos subidas por los clientes.
 * También asegura que todos los textos y reportes salgan siempre con tildes y eñes bien escritas.
 */
package com.tecnicsecure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TecniSecureProperties propiedades;

    // Recibe los ajustes de la tienda para saber en qué carpeta se guardan las fotos subidas
    public WebMvcConfig(TecniSecureProperties propiedades) {
        this.propiedades = propiedades;
    }

    // Respeta el formato de respuesta que solicita normalmente el navegador del usuario
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false);
    }

    // Asegura que todos los textos, tablas y respuestas salgan con tildes y la letra ñ bien formadas
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Garantiza que textos y archivos de hoja de cálculo respeten los acentos al abrirse
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter texto) {
                texto.setDefaultCharset(StandardCharsets.UTF_8);
            }
            if (converter instanceof MappingJackson2HttpMessageConverter json) {
                // Agrega el soporte de idioma español sin quitar los formatos que usan otros módulos internos
                List<MediaType> tipos = new java.util.ArrayList<>(json.getSupportedMediaTypes());
                MediaType utf8 = new MediaType("application", "json", StandardCharsets.UTF_8);
                if (!tipos.contains(utf8)) {
                    tipos.add(utf8);
                }
                json.setSupportedMediaTypes(tipos);
            }
        }
    }

    // Conecta las direcciones que pide el navegador con las carpetas reales de pantallas e imágenes
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo publica los archivos visibles de la página, dejando libres las direcciones internas del servidor
        String raizFrontend = rutaFrontend();
        // Adapta la ruta de carpeta del computador al formato de dirección que entiende el servidor web
        String base = comoUrl(raizFrontend);

        // Publica cada carpeta visual del sitio: estilos, interactividad, fotos, videos, audios y paneles
        String[][] carpetas = {
                {"/css/**", "css"}, {"/js/**", "js"}, {"/img/**", "img"}, {"/video/**", "video"},
                {"/audio/**", "audio"}, {"/pages/**", "pages"}, {"/panel/**", "panel"}
        };
        for (String[] carpeta : carpetas) {
            registry.addResourceHandler(carpeta[0])
                    .addResourceLocations(base + carpeta[1] + "/")
                    .resourceChain(true)
                    .addResolver(new ResolutorCarpetas());
        }

        // Permite abrir cualquier página del sitio ya sea escribiendo la dirección directa o desde un enlace interno
        registry.addResourceHandler("/**")
                .addResourceLocations(base, "classpath:/static/")
                .resourceChain(true)
                .addResolver(new ResolutorCarpetas());

        // Hace visibles en la web las fotos de perfil, productos y comprobantes subidos por los usuarios
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(comoUrl(rutaUploads()))
                .resourceChain(true)
                .addResolver(new ResolutorCarpetas());
    }

    // Ayuda a abrir automáticamente la portada de una sección cuando el usuario entra solo con el nombre de la carpeta
    private static class ResolutorCarpetas extends org.springframework.web.servlet.resource.PathResourceResolver {
        // Si alguien entra a una dirección de carpeta (por ejemplo /panel/administrador), abre su página principal
        @Override
        protected org.springframework.core.io.Resource getResource(String rutaRecurso,
                org.springframework.core.io.Resource ubicacion) throws java.io.IOException {
            org.springframework.core.io.Resource recurso = super.getResource(rutaRecurso, ubicacion);
            if (recurso == null) {
                // Completa la dirección añadiendo "index.html" al final
                String ruta = rutaRecurso.isEmpty()
                        ? "index.html"
                        : (rutaRecurso.endsWith("/") ? rutaRecurso : rutaRecurso + "/") + "index.html";
                recurso = super.getResource(ruta, ubicacion);
            }
            return recurso;
        }
    }

    // Busca la ubicación real de la carpeta donde están guardadas las pantallas de la tienda
    private String rutaFrontend() {
        String configurada = System.getProperty("frontend.dir");
        if (configurada != null && !configurada.isBlank()) {
            return rutaAbsoluta(configurada);
        }
        return rutaAbsoluta(java.nio.file.Path.of("..", "frontend").toString());
    }

    // Determina la carpeta real donde se guardan las fotos subidas por clientes y administradores
    private String rutaUploads() {
        String configurada = propiedades.uploads() != null ? propiedades.uploads().dir() : null;
        if (configurada != null && !configurada.isBlank()) {
            return rutaAbsoluta(configurada);
        }
        return rutaFrontend() + java.io.File.separator + "uploads";
    }

    // Convierte una ubicación corta de carpeta en su dirección completa dentro del disco
    private String rutaAbsoluta(String ruta) {
        return java.nio.file.Path.of(ruta).toAbsolutePath().normalize().toString();
    }

    // Da formato de dirección web local a una carpeta del computador para poder mostrar sus archivos
    private String comoUrl(String ruta) {
        String normalizada = ruta.replace('\\', '/');
        if (!normalizada.endsWith("/")) {
            normalizada = normalizada + "/";
        }
        return normalizada.startsWith("file:") ? normalizada : "file:" + normalizada;
    }
}
