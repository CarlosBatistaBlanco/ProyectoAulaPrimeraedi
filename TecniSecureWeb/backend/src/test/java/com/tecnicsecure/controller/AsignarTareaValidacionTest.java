/*
 * Este archivo contiene pruebas automáticas para la pantalla donde el administrador
 * asigna tareas a los empleados. Comprueba que, si falta información obligatoria o
 * la fecha límite está mal escrita, el sistema rechace el envío con un aviso claro.
 */
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

@SpringBootTest
@AutoConfigureMockMvc
class AsignarTareaValidacionTest {

    @Autowired
    private MockMvc mockMvc;

    // Comprueba que no se permita crear una tarea si no se eligió al empleado responsable
    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void sinEmpleadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Tarea\"}"))
                .andExpect(status().isBadRequest());
    }

    // Comprueba que el sistema exija escribir un título antes de guardar la tarea
    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void sinTituloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEmpleadoUsuario\":1}"))
                .andExpect(status().isBadRequest());
    }

    // Comprueba que una fecha límite escrita con un formato equivocado sea rechazada de forma clara
    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void fechaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/gestion-empleados/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEmpleadoUsuario\":1,\"titulo\":\"T\",\"fechaLimite\":\"01-12-2026\"}"))
                .andExpect(status().isBadRequest());
    }
}
