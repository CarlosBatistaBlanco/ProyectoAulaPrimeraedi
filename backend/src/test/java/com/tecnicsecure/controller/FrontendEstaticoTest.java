package com.tecnicsecure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El frontend se sirve desde una carpeta externa al jar: estas pruebas
 * garantizan que los recursos siguen respondiendo 200 (y no 500/404) sin
 * depender del directorio de trabajo ni del sistema operativo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FrontendEstaticoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sirvePaginasYRecursosDelFrontend() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));

        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/pages/lista_deseos.html"))
                .andExpect(status().isOk());
    }

    @Test
    void recursoInexistenteDevuelve404YNo500() throws Exception {
        mockMvc.perform(get("/no-existe.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    void carpetaSinArchivoDevuelveSuIndex() throws Exception {
        // /panel/administrador y /panel/administrador/ deben abrir el index.html
        mockMvc.perform(get("/panel/administrador"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(get("/panel/administrador/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/panel/cliente"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/panel/empleado"))
                .andExpect(status().isOk());
        // Las rutas que no son carpetas siguen dando 404
        mockMvc.perform(get("/panel/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthRespondesinAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
