/*
 * Este archivo contiene pruebas automáticas que comprueban que las páginas visibles
 * del sitio web y los paneles de cada tipo de usuario abran correctamente. También
 * verifica que las direcciones inexistentes muestren el aviso de página no encontrada.
 */
package com.tecnicsecure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendEstaticoTest {

    @Autowired
    private MockMvc mockMvc;

    // Comprueba que la página de inicio, los archivos de diseño, la lógica de pantalla y la lista de deseos carguen bien
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

    // Comprueba que abrir una dirección que no existe muestre el aviso de no encontrado y no un error interno
    @Test
    void recursoInexistenteDevuelve404YNo500() throws Exception {
        mockMvc.perform(get("/no-existe.html"))
                .andExpect(status().isNotFound());
    }

    // Comprueba que los accesos directos a los paneles de administrador, cliente y empleado abran su pantalla principal
    @Test
    void carpetaSinArchivoDevuelveSuIndex() throws Exception {
        // Las direcciones de cada panel deben abrir directamente su portada sin escribir el nombre del archivo
        mockMvc.perform(get("/panel/administrador"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(get("/panel/administrador/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/panel/cliente"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/panel/empleado"))
                .andExpect(status().isOk());
        // Una dirección de panel inventada debe seguir respondiendo que no existe
        mockMvc.perform(get("/panel/inexistente"))
                .andExpect(status().isNotFound());
    }

    // Comprueba que la verificación de estado del sistema responda correctamente sin exigir inicio de sesión
    @Test
    void healthRespondesinAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
