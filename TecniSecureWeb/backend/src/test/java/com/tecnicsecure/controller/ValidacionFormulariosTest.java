/*
 * Este archivo contiene pruebas automáticas para los formularios públicos de registro
 * y contacto. Garantiza que el sistema rechace datos mal escritos —como teléfonos o
 * documentos con letras, correos incompletos o contraseñas débiles— antes de guardarlos.
 */
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

@SpringBootTest
@AutoConfigureMockMvc
class ValidacionFormulariosTest {

    @Autowired
    private MockMvc mockMvc;

    // Arma un formulario de registro de ejemplo con los valores que se quieren poner a prueba
    private String registro(String documento, String correo, String telefono, String nombre, String password) {
        return """
                {"nombre":"%s","documento":"%s","correo":"%s","telefono":"%s","password":"%s"}
                """.formatted(nombre, documento, correo, telefono, password);
    }

    // Comprueba que el formulario de registro rechace un número de teléfono que contenga letras
    @Test
    @DisplayName("El telefono con letras se rechaza")
    void telefonoConLetrasRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida1@gmail.com", "abc1234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefono").exists());
    }

    // Comprueba que el registro rechace un documento de identidad que incluya letras
    @Test
    @DisplayName("El documento con letras se rechaza")
    void documentoConLetrasRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("ABC12345", "valida2@gmail.com", "3001234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").exists());
    }

    // Comprueba que el registro rechace correos incompletos a los que les falta la terminación, como ".com"
    @Test
    @DisplayName("El correo incompleto (sin .com) se rechaza")
    void correoSinDominioRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "ana@gmail", "3001234567", "Ana Lopez", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    // Comprueba que no se permitan números mezclados dentro del nombre completo
    @Test
    @DisplayName("El nombre con numeros se rechaza")
    void nombreConNumerosRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida3@gmail.com", "3001234567", "Ana123", "Prueba123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    // Comprueba que el registro bloquee contraseñas demasiado simples, como "123456"
    @Test
    @DisplayName("La contrasena debil se rechaza")
    void passwordDebilRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("12345678", "valida4@gmail.com", "3001234567", "Ana Lopez", "123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    // Comprueba que el formulario de contacto también rechace un número de teléfono con letras
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
