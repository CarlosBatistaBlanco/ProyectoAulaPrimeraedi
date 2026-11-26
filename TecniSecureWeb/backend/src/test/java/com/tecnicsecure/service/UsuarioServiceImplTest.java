/*
 * Este archivo contiene pruebas automáticas para la administración de cuentas y el
 * historial de actividad en el panel administrativo. Comprueba la reactivación de
 * cuentas, que nadie desactive su propia cuenta, el bloqueo de correos repetidos y la limpieza de historial.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.auth.UsuarioAdminRequest;
import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.UsuarioServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private LogActividadRepository logRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioActual usuarioActual;

    @InjectMocks
    private UsuarioServiceImpl service;

    private Usuario admin;
    private Usuario clienteUser;
    private Rol rolCliente;

    // Prepara una cuenta administradora activa y una cuenta cliente desactivada de ejemplo antes de cada prueba
    @BeforeEach
    void preparar() {
        admin = new Usuario();
        admin.setIdUsuario(1);
        admin.setNombre("Admin Principal");
        admin.setCorreo("admin@tecnisecure.com");
        admin.setActivo(true);

        rolCliente = new Rol();
        rolCliente.setIdRol(3);
        rolCliente.setNombre("cliente");

        clienteUser = new Usuario();
        clienteUser.setIdUsuario(5);
        clienteUser.setNombre("Juan Perez");
        clienteUser.setCorreo("juan@correo.com");
        clienteUser.setRol(rolCliente);
        clienteUser.setActivo(false);
    }

    // Comprueba que reactivar una cuenta deshabilitada vuelva a permitir su ingreso y deje constancia en el historial
    @Test
    void reactivarUsuarioDesactivadoDebeActivarYRegistrarLog() {
        when(usuarioActual.requerir()).thenReturn(admin);
        when(usuarioActual.idOpcional()).thenReturn(1);
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(clienteUser));

        service.cambiarEstado(5, true);

        assertTrue(clienteUser.getActivo());
        verify(usuarioRepository).save(clienteUser);
        verify(logRepository).save(any(LogActividad.class));
    }

    // Comprueba que un administrador no pueda desactivar su propia cuenta por accidente
    @Test
    void desactivarPropiaCuentaDebeFallar() {
        when(usuarioActual.requerir()).thenReturn(admin);

        ApiException ex = assertThrows(ApiException.class, () -> service.cambiarEstado(1, false));
        assertTrue(ex.getMessage().toLowerCase().contains("propia cuenta"));
    }

    // Comprueba que el sistema rechace crear una cuenta nueva usando un correo electrónico que ya está registrado
    @Test
    void crearUsuarioConCorreoDuplicadoDebeFallar() {
        UsuarioAdminRequest req = new UsuarioAdminRequest(
                "Nuevo", "juan@correo.com", "123", "300", "cliente", "Clave123!", true, false);
        when(usuarioRepository.existsByCorreoIgnoreCase("juan@correo.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> service.crear(req));
    }

    // Comprueba que los filtros del historial y la limpieza de registros antiguos funcionen y dejen constancia de la acción
    @Test
    void auditoriaFiltradaYDepuracionMinimo30Dias() {
        when(logRepository.buscarFiltrado(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(logRepository.deleteByCreadoEnBefore(any())).thenReturn(4);
        when(usuarioActual.idOpcional()).thenReturn(1);

        var page = service.auditoria("login", null, LocalDate.now().minusDays(7), LocalDate.now(), PageRequest.of(0, 10));
        int eliminados = service.depurarAuditoria(10);

        assertEquals(0, page.contenido().size());
        assertEquals(4, eliminados);
        verify(logRepository).save(any(LogActividad.class));
    }
}
