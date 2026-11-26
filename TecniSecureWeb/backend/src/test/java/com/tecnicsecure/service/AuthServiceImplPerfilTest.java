/*
 * Este archivo contiene pruebas automáticas para la consulta del perfil de usuario
 * después de iniciar sesión. Comprueba que los clientes reciban su identificador de
 * comprador para poder usar el chat en vivo, y que los administradores no lo muestren.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.config.TecniSecureProperties;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.repository.ClienteRepository;
import com.tecnicsecure.repository.LogActividadRepository;
import com.tecnicsecure.repository.RolRepository;
import com.tecnicsecure.repository.TokenRecuperacionRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import com.tecnicsecure.security.JwtService;
import com.tecnicsecure.service.impl.AuthServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService · idCliente en el perfil")
class AuthServiceImplPerfilTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private TokenRecuperacionRepository tokenRepository;
    @Mock private LogActividadRepository logRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TecniSecureProperties propiedades;
    @Mock private UsuarioActual usuarioActual;

    @InjectMocks private AuthServiceImpl authService;

    // Prepara una cuenta de ejemplo con el perfil que se necesita probar
    private Usuario usuarioConRol(String nombreRol, int idUsuario) {
        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setNombre(nombreRol);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombre("Prueba");
        usuario.setCorreo("prueba@ejemplo.com");
        usuario.setRol(rol);
        return usuario;
    }

    // Comprueba que una cuenta de cliente reciba su número de comprador para abrir el chat en directo
    @Test
    @DisplayName("Un cliente recibe su idCliente para poder suscribirse al chat")
    void clienteRecibeIdCliente() {
        Usuario usuario = usuarioConRol(Rol.CLIENTE, 10);
        Cliente cliente = new Cliente();
        cliente.setIdCliente(77);

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByUsuarioIdUsuario(10)).thenReturn(Optional.of(cliente));

        UsuarioResponse respuesta = authService.perfil(10);

        assertThat(respuesta.idCliente()).isEqualTo(77);
        assertThat(respuesta.rol()).isEqualTo(Rol.CLIENTE);
    }

    // Comprueba que si un cliente todavía no tiene ficha comercial creada, su perfil se entregue vacío en ese dato sin errores
    @Test
    @DisplayName("Un usuario sin ficha de cliente devuelve idCliente nulo")
    void clienteSinFichaDevuelveNulo() {
        Usuario usuario = usuarioConRol(Rol.CLIENTE, 11);

        when(usuarioRepository.findById(11)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByUsuarioIdUsuario(11)).thenReturn(Optional.empty());

        assertThat(authService.perfil(11).idCliente()).isNull();
    }

    // Comprueba que al consultar el perfil de un administrador nunca se incluya un número de cliente
    @Test
    @DisplayName("El administrador nunca expone idCliente")
    void administradorNoExponeIdCliente() {
        Usuario usuario = usuarioConRol(Rol.ADMINISTRADOR, 1);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertThat(authService.perfil(1).idCliente()).isNull();
    }
}
