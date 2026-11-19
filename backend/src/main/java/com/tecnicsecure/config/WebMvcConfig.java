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

/**
 * Configuración MVC (la "C" del patrón MVC):
 * — garantiza JSON y texto siempre en UTF-8 (ñ, tildes y símbolos);
 * — publica /uploads e /img desde el frontend;
 * — sirve index.html en la raíz, sustituyendo a Apache.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TecniSecureProperties propiedades;

    public WebMvcConfig(TecniSecureProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Texto plano y CSV en UTF-8 (Excel y navegadores respetan los acentos)
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter texto) {
                texto.setDefaultCharset(StandardCharsets.UTF_8);
            }
            if (converter instanceof MappingJackson2HttpMessageConverter json) {
                // Se AGREGAN tipos: reemplazar la lista deja fuera los tipos que
                // esperan Actuator y otros componentes.
                List<MediaType> tipos = new java.util.ArrayList<>(json.getSupportedMediaTypes());
                MediaType utf8 = new MediaType("application", "json", StandardCharsets.UTF_8);
                if (!tipos.contains(utf8)) {
                    tipos.add(utf8);
                }
                json.setSupportedMediaTypes(tipos);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo los recursos del frontend: no debe capturar /api, /actuator ni Swagger.
        String raizFrontend = rutaFrontend();
        // File.separator solo vale dentro del sistema de archivos; la URL
        // necesita "file:" con barras.
        String base = comoUrl(raizFrontend);

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

        // Todos los recursos deben resolverse tanto por ruta directa
        // (/panel/administrador/...) como relativa a la profundidad de la URL
        // (/panel/administrador/index.html usa ../../css/...).
        registry.addResourceHandler("/**")
                .addResourceLocations(base, "classpath:/static/")
                .resourceChain(true)
                .addResolver(new ResolutorCarpetas());

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(comoUrl(rutaUploads()))
                .resourceChain(true)
                .addResolver(new ResolutorCarpetas());
    }

    /**
     * Permite entrar a una carpeta sin escribir el archivo:
     * /panel/administrador -> /panel/administrador/index.html
     */
    private static class ResolutorCarpetas extends org.springframework.web.servlet.resource.PathResourceResolver {
        @Override
        protected org.springframework.core.io.Resource getResource(String rutaRecurso,
                org.springframework.core.io.Resource ubicacion) throws java.io.IOException {
            org.springframework.core.io.Resource recurso = super.getResource(rutaRecurso, ubicacion);
            if (recurso == null) {
                // "panel/administrador" -> "panel/administrador/index.html"
                // "" (la carpeta misma) -> "index.html"
                String ruta = rutaRecurso.isEmpty()
                        ? "index.html"
                        : (rutaRecurso.endsWith("/") ? rutaRecurso : rutaRecurso + "/") + "index.html";
                recurso = super.getResource(ruta, ubicacion);
            }
            return recurso;
        }
    }

    /** Ruta absoluta de la carpeta frontend (hermana de backend o -Dfrontend.dir). */
    private String rutaFrontend() {
        String configurada = System.getProperty("frontend.dir");
        if (configurada != null && !configurada.isBlank()) {
            return rutaAbsoluta(configurada);
        }
        return rutaAbsoluta(java.nio.file.Path.of("..", "frontend").toString());
    }

    private String rutaUploads() {
        String configurada = propiedades.uploads() != null ? propiedades.uploads().dir() : null;
        if (configurada != null && !configurada.isBlank()) {
            return rutaAbsoluta(configurada);
        }
        return rutaFrontend() + java.io.File.separator + "uploads";
    }

    /** Convierte una ruta relativa (al directorio de trabajo) en absoluta. */
    private String rutaAbsoluta(String ruta) {
        return java.nio.file.Path.of(ruta).toAbsolutePath().normalize().toString();
    }

    private String comoUrl(String ruta) {
        String normalizada = ruta.replace('\\', '/');
        if (!normalizada.endsWith("/")) {
            normalizada = normalizada + "/";
        }
        return normalizada.startsWith("file:") ? normalizada : "file:" + normalizada;
    }
}
