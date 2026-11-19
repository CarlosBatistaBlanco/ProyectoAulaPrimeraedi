package com.tecnicsecure.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los formularios publicos no deben aceptar letras en telefono/documento,
 * ni correos sin dominio completo. El backend es la ultima barrera aunque
 * alguien se salte el JavaScript del navegador.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidacionFormulariosTest {

    @Autowired
    private MockMvc mockMvc;

    private String registro(String documento, String correo, String telefono, String nombre, String password) {
        return """
                {"nombre":"%s","documento":"%s","correo":"%s","telefono":"%s","password":"%s"}
                """.formatted(nombre, documento, correo, telefono, password);
    }

    @Test
    @DisplayName("El telefono con letras se rechaza")
    void telefonoConLetrasRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida1@gmail.com", "abc1234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefono").exists());
    }

    @Test
    @DisplayName("El documento con letras se rechaza")
    void documentoConLetrasRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("ABC12345", "valida2@gmail.com", "3001234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").exists());
    }

    @Test
    @DisplayName("El correo incompleto (sin .com) se rechaza")
    void correoSinDominioRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "ana@gmail", "3001234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    @Test
    @DisplayName("El nombre con numeros se rechaza")
    void nombreConNumerosRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida3@gmail.com", "3001234567", "Ana123", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    @DisplayName("La contrasena debil se rechaza")
    void passwordDebilRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida4@gmail.com", "3001234567", "Ana Lopez", "123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    @DisplayName("El contacto con telefono invalido se rechaza")
    void contactoTelefonoInvalido() throws Exception {
        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Lopez","correo":"ana@gmail.com","telefono":"abc",
                                 "asunto":"Consulta","mensaje":"Hola"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
