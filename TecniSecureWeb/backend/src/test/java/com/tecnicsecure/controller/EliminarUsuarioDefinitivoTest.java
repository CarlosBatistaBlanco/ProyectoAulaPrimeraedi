/*
 * Este archivo contiene pruebas automáticas de seguridad para la opción de borrar
 * una cuenta de forma definitiva en el panel administrativo. Garantiza que nadie
 * pueda eliminar un usuario sin confirmar primero con la contraseña del administrador.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.repository.RolRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EliminarUsuarioDefinitivoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    // Borra la cuenta temporal al finalizar cada prueba para no dejar datos sobrantes
    @AfterEach
    void limpiar() {
        usuarioRepository.findByCorreoIgnoreCase("clitest-def@ejemplo.com")
                .ifPresent(usuarioRepository::delete);
    }

    // Crea o busca una cuenta de cliente de prueba para intentar eliminarla durante las verificaciones
    private Integer idDePrueba() {
        return usuarioRepository.findByCorreoIgnoreCase("clitest-def@ejemplo.com")
                .map(Usuario::getIdUsuario)
                .orElseGet(() -> {
                    Rol rol = rolRepository.findAll().stream()
                            .filter(r -> "cliente".equalsIgnoreCase(r.getNombre()))
                            .findFirst()
                            .orElseThrow();
                    Usuario usuario = new Usuario();
                    usuario.setNombre("Usuario Borrado QA");
                    usuario.setCorreo("clitest-def@ejemplo.com");
                    usuario.setDocumento("QA-DEF-1");
                    usuario.setPasswordHash("{noop}Prueba123!");
                    usuario.setRol(rol);
                    usuario.setActivo(true);
                    return usuarioRepository.save(usuario).getIdUsuario();
                });
    }

    // Comprueba que la cuenta siga intacta si el administrador intenta borrarla sin escribir su contraseña
    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void sinClaveNoElimina() throws Exception {
        Integer id = idDePrueba();
        mockMvc.perform(delete("/api/usuarios/" + id + "/definitivo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findById(id)).isPresent();
    }

    // Comprueba que la cuenta tampoco se borre si el administrador escribe una contraseña equivocada
    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void claveIncorrectaNoElimina() throws Exception {
        Integer id = idDePrueba();
        mockMvc.perform(delete("/api/usuarios/" + id + "/definitivo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clave\":\"clave-que-no-es\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findById(id)).isPresent();
    }
}
