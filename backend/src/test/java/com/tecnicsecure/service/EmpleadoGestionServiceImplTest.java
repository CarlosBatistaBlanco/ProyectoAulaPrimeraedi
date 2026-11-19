package com.tecnicsecure.service;

import com.tecnicsecure.entity.EmpleadoPerfil;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.EmpleadoGestionServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoGestionServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpleadoPerfilRepository perfilRepository;
    @Mock private TareaEmpleadoRepository tareaRepository;
    @Mock private LogActividadRepository logRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private SoporteService soporteService;
    @Mock private UsuarioActual usuarioActual;

    @InjectMocks
    private EmpleadoGestionServiceImpl service;

    private Usuario empleado;
    private Usuario admin;

    @BeforeEach
    void setup() {
        Rol rolEmp = new Rol();
        rolEmp.setNombre("empleado");
        empleado = new Usuario();
        empleado.setIdUsuario(10);
        empleado.setNombre("Carlos Tecnico");
        empleado.setCorreo("carlos@tecnisecure.com");
        empleado.setRol(rolEmp);

        admin = new Usuario();
        admin.setIdUsuario(1);
        admin.setNombre("Admin");
    }

    @Test
    void actualizarPermisosGuardaAreaYModulosObligatorios() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(perfilRepository.findByIdUsuario(10)).thenReturn(Optional.empty());
        when(usuarioActual.idOpcional()).thenReturn(1);

        var res = service.actualizarPermisos(10, "VENTAS", List.of("ventas", "chat"));

        assertEquals("VENTAS", res.get("area"));
        @SuppressWarnings("unchecked")
        List<String> mods = (List<String>) res.get("modulos");
        assertTrue(mods.contains("dashboard"));
        assertTrue(mods.contains("tareas"));
        assertTrue(mods.contains("ventas"));
        verify(perfilRepository).save(any(EmpleadoPerfil.class));
        verify(logRepository).save(any());
    }

    @Test
    void asignarYCalificarTareaFlujoCompleto() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioActual.idOpcional()).thenReturn(1);

        TareaEmpleado t = service.asignarTarea(10, "Revisar stock", "Conteo rapido", "alta", LocalDate.now().plusDays(1));
        assertEquals("pendiente", t.getEstado());
        assertEquals("alta", t.getPrioridad());

        t.setIdTarea(20);
        when(tareaRepository.findById(20)).thenReturn(Optional.of(t));
        TareaEmpleado revisada = service.calificarTarea(20, 5, "Excelente orden");

        assertEquals("revisada", revisada.getEstado());
        assertEquals(5, revisada.getCalificacion());
        verify(soporteService, times(2)).notificar(eq(10), any(), any(), any());
    }

    @Test
    void empleadoAjenoNoPuedeActualizarTareaDeOtro() {
        Usuario otro = new Usuario();
        otro.setIdUsuario(99);
        TareaEmpleado t = new TareaEmpleado();
        t.setIdTarea(33);
        t.setIdEmpleadoUsuario(10);

        when(usuarioActual.requerir()).thenReturn(otro);
        when(usuarioActual.esAdministrador()).thenReturn(false);
        when(tareaRepository.findById(33)).thenReturn(Optional.of(t));

        assertThrows(ApiException.class, () -> service.actualizarProgreso(33, "completada", "ok", "ok"));
    }
}
