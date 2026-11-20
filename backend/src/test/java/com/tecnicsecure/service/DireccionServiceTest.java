package com.tecnicsecure.service;

import com.tecnicsecure.dto.direccion.DireccionRequest;
import com.tecnicsecure.dto.direccion.DireccionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Direcciones de entrega: cada usuario administra las suyas y solo una puede
 * quedar como predeterminada (es la que precarga el checkout).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DireccionServiceTest {

    @Autowired private DireccionService servicio;

    private DireccionRequest dir(String etiqueta, String direccion, Boolean predeterminada) {
        return new DireccionRequest(etiqueta, direccion, "Bogotá", "Chapinero",
                "3001234567", "Ana Pérez", "Torre 2 apto 501", null, null, predeterminada);
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("La primera direccion queda predeterminada automaticamente")
    void primeraEsPredeterminada() {
        DireccionResponse creada = servicio.crear(dir("Casa", "Calle 45 # 12-34", false));

        assertThat(creada.idDireccion()).isNotNull();
        assertThat(creada.predeterminada()).isTrue();
        assertThat(creada.etiqueta()).isEqualTo("Casa");
        assertThat(creada.telefono()).isEqualTo("3001234567");
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Solo puede haber una predeterminada a la vez")
    void soloUnaPredeterminada() {
        servicio.crear(dir("Casa", "Calle 45 # 12-34", false));
        DireccionResponse oficina = servicio.crear(dir("Oficina", "Carrera 7 # 71-21", true));

        assertThat(oficina.predeterminada()).isTrue();
        long predeterminadas = servicio.misDirecciones().stream()
                .filter(DireccionResponse::predeterminada).count();
        assertThat(predeterminadas).isEqualTo(1);

        DireccionResponse principal = servicio.direccionPrincipal();
        assertThat(principal.etiqueta()).isEqualTo("Oficina");
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Se puede editar la direccion para corregirla")
    void editarDireccion() {
        DireccionResponse creada = servicio.crear(dir("Casa", "Calle 45 # 12-34", false));
        DireccionResponse editada = servicio.actualizar(creada.idDireccion(),
                dir("Casa", "Calle 50 # 20-10, Bogotá", true));

        assertThat(editada.direccion()).contains("Calle 50");
        assertThat(servicio.misDirecciones()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Al borrar la predeterminada otra toma el relevo")
    void eliminarPredeterminadaReasigna() {
        DireccionResponse casa = servicio.crear(dir("Casa", "Calle 45 # 12-34", false));
        servicio.crear(dir("Oficina", "Carrera 7 # 71-21", false));

        servicio.eliminar(casa.idDireccion());

        var restantes = servicio.misDirecciones();
        assertThat(restantes).hasSize(1);
        assertThat(restantes.get(0).predeterminada()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Rechaza una direccion demasiado corta")
    void rechazaDireccionCorta() {
        assertThatThrownBy(() -> servicio.crear(dir("Casa", "Calle 1", false)))
                .hasMessageContaining("al menos 8");
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Sin direcciones, la principal es nula (el checkout queda vacio)")
    void sinDireccionesNoHayPrincipal() {
        assertThat(servicio.direccionPrincipal()).isNull();
        assertThat(servicio.misDirecciones()).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com")
    @DisplayName("Marcar predeterminada mueve la marca")
    void marcarPredeterminada() {
        var a = servicio.crear(dir("Casa", "Calle 45 # 12-34", false));
        var b = servicio.crear(dir("Oficina", "Carrera 7 # 71-21", false));

        servicio.marcarPredeterminada(b.idDireccion());

        var lista = servicio.misDirecciones();
        assertThat(lista.stream().filter(DireccionResponse::predeterminada).findFirst()
                .orElseThrow().idDireccion()).isEqualTo(b.idDireccion());
        assertThat(lista.stream().anyMatch(d -> d.idDireccion().equals(a.idDireccion()))).isTrue();
    }
}
