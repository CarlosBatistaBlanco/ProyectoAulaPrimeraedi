package com.tecnicsecure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los datos invalidos deben responder 400 (no 500) para que el frontend
 * pueda mostrar el mensaje correcto en lugar de un "error interno".
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsignarTareaValidacionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void sinEmpleadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Tarea\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void sinTituloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEmpleadoUsuario\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void fechaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEmpleadoUsuario\":1,\"titulo\":\"T\",\"fechaLimite\":\"01-12-2026\"}"))
                .andExpect(status().isBadRequest());
    }
}
