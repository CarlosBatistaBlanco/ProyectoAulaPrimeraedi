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

/**
 * El borrado definitivo exige la clave del administrador: sin clave o con
 * clave incorrecta debe rechazarse (400) y el usuario debe seguir existiendo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EliminarUsuarioDefinitivoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    /** No deja datos de prueba en la base. */
    @AfterEach
    void limpiar() {
        usuarioRepository.findByCorreoIgnoreCase("clitest-def@ejemplo.com")
                .ifPresent(usuarioRepository::delete);
    }

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
