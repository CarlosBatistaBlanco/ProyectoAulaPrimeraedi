package com.tecnicsecure.service;

import com.tecnicsecure.entity.EmpleadoPerfil;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.TareaEmpleado;
import com.tecnicsecure.entity.TareaHistorialEntrega;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.EmpleadoGestionServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock private TareaHistorialEntregaRepository historialRepository;
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

    @Test
    void empleadoNoPuedeModificarTareaYaCompletadaNiVolverAEnProgreso() {
        TareaEmpleado t = new TareaEmpleado();
        t.setIdTarea(40);
        t.setIdEmpleadoUsuario(10);
        t.setEstado("completada");
        t.setEvidencia("/uploads/evidencias/entrega1.pdf");

        when(usuarioActual.requerir()).thenReturn(empleado);
        when(usuarioActual.esAdministrador()).thenReturn(false);
        when(tareaRepository.findById(40)).thenReturn(Optional.of(t));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.actualizarProgreso(40, "en_progreso", null, "Quiero cambiarla"));
        assertTrue(ex.getMessage().contains("espera de revisión"));
        verify(tareaRepository, never()).save(any());
    }

    @Test
    void completarArchivaEntregaYDevolverPermiteSegundoIntentoSinPerderElPrimero() {
        TareaEmpleado t = new TareaEmpleado();
        t.setIdTarea(50);
        t.setIdEmpleadoUsuario(10);
        t.setTitulo("Instalar camaras");
        t.setEstado("en_progreso");

        when(usuarioActual.requerir()).thenReturn(empleado);
        when(usuarioActual.esAdministrador()).thenReturn(false);
        when(tareaRepository.findById(50)).thenReturn(Optional.of(t));
        when(historialRepository.countByIdTarea(50)).thenReturn(0L);
        when(usuarioRepository.findByRolNombre(Rol.ADMINISTRADOR)).thenReturn(List.of(admin));

        // 1. Empleado marca como completada -> se archiva intento #1
        service.actualizarProgreso(50, "completada", "/uploads/evidencias/intento1.pdf", "Primera entrega");
        assertEquals("completada", t.getEstado());

        ArgumentCaptor<TareaHistorialEntrega> captor = ArgumentCaptor.forClass(TareaHistorialEntrega.class);
        verify(historialRepository).save(captor.capture());
        TareaHistorialEntrega entrega1 = captor.getValue();
        assertEquals(1, entrega1.getNumeroIntento());
        assertEquals("/uploads/evidencias/intento1.pdf", entrega1.getEvidencia());
        assertEquals("en_revision", entrega1.getEstadoRevision());

        // 2. Administrador devuelve para corrección -> intento #1 queda archivado como "devuelta" y tarea se reabre
        when(historialRepository.findFirstByIdTareaOrderByNumeroIntentoDesc(50)).thenReturn(Optional.of(entrega1));
        when(usuarioActual.idOpcional()).thenReturn(1);
        service.revisarYCalificarTarea(50, "en_progreso", 2, "Falta foto del tablero");

        assertEquals("en_progreso", t.getEstado());
        assertNull(t.getEvidencia());
        assertEquals("devuelta", entrega1.getEstadoRevision());
        assertEquals("Falta foto del tablero", entrega1.getObservacionAdmin());

        // 3. Empleado vuelve a completar con nuevo archivo -> se archiva intento #2
        when(historialRepository.countByIdTarea(50)).thenReturn(1L);
        service.actualizarProgreso(50, "completada", "/uploads/evidencias/intento2.pdf", "Corregido con foto");

        assertEquals("completada", t.getEstado());
        assertEquals("/uploads/evidencias/intento2.pdf", t.getEvidencia());
        verify(historialRepository, times(3)).save(any(TareaHistorialEntrega.class));
    }
}
