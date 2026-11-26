/*
 * Este archivo ejecuta las reglas de la libreta de direcciones de cada usuario: valida que la
 * direccion y el telefono esten bien escritos, evita que nadie edite direcciones ajenas y se
 * asegura de que siempre exista una direccion favorita lista para usar al confirmar una compra.
 * Su trabajo se nota en el menu "Direccion" de los paneles y en el formulario de envio al pagar.
 */
package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.direccion.DireccionRequest;
import com.tecnicsecure.dto.direccion.DireccionResponse;
import com.tecnicsecure.entity.DireccionUsuario;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.DireccionUsuarioRepository;
import com.tecnicsecure.service.DireccionService;
import com.tecnicsecure.util.PatronesValidacion;
import com.tecnicsecure.util.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DireccionServiceImpl implements DireccionService {

    // Limite de 15 direcciones por cuenta para mantener ordenada la libreta de envios.
    private static final int MAXIMO_POR_USUARIO = 15;

    private final DireccionUsuarioRepository repositorio;
    private final UsuarioActual usuarioActual;

    // Prepara el acceso a las direcciones guardadas y a la cuenta que tiene la sesion abierta.
    public DireccionServiceImpl(DireccionUsuarioRepository repositorio, UsuarioActual usuarioActual) {
        this.repositorio = repositorio;
        this.usuarioActual = usuarioActual;
    }

    // Muestra todas las direcciones de la persona conectada poniendo primero la marcada como favorita.
    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponse> misDirecciones() {
        Usuario usuario = usuarioActual.requerir();
        return repositorio.findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(usuario.getIdUsuario())
                .stream().map(DireccionResponse::de).toList();
    }

    // Guarda una nueva direccion revisando el tope maximo de 15 y marcandola como favorita si es la primera o si asi se pidio.
    @Override
    @Transactional
    public DireccionResponse crear(DireccionRequest request) {
        Usuario usuario = usuarioActual.requerir();
        validar(request);

        // Avisa si el usuario ya llego al limite de direcciones permitidas.
        if (repositorio.countByIdUsuario(usuario.getIdUsuario()) >= MAXIMO_POR_USUARIO) {
            throw ApiException.badRequest("Ya tienes " + MAXIMO_POR_USUARIO
                    + " direcciones guardadas. Elimina una para agregar otra.");
        }

        DireccionUsuario entidad = new DireccionUsuario();
        entidad.setIdUsuario(usuario.getIdUsuario());
        aplicar(entidad, request);

        // Si es la primera direccion de la cuenta, queda marcada automaticamente como favorita.
        boolean primera = repositorio.countByIdUsuario(usuario.getIdUsuario()) == 0;
        boolean marcar = Boolean.TRUE.equals(request.predeterminada()) || primera;

        repositorio.save(entidad);
        if (marcar) {
            fijarPredeterminada(usuario.getIdUsuario(), entidad.getIdDireccion());
            entidad.setPredeterminada(true);
        }
        return DireccionResponse.de(entidad);
    }

    // Edita una direccion verificando primero que realmente pertenezca a esta persona y manteniendo siempre una favorita.
    @Override
    @Transactional
    public DireccionResponse actualizar(Integer idDireccion, DireccionRequest request) {
        Usuario usuario = usuarioActual.requerir();
        // Comprueba que la direccion realmente sea de esta cuenta para proteger la privacidad.
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        validar(request);

        aplicar(entidad, request);
        entidad.setActualizadoEn(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.predeterminada())) {
            fijarPredeterminada(usuario.getIdUsuario(), idDireccion);
            entidad.setPredeterminada(true);
        } else if (Boolean.FALSE.equals(request.predeterminada())
                && Boolean.TRUE.equals(entidad.getPredeterminada())) {
            // Si es la unica direccion de la cuenta, se mantiene como favorita para que el envio siempre tenga datos.
            long total = repositorio.countByIdUsuario(usuario.getIdUsuario());
            if (total == 1) {
                entidad.setPredeterminada(true);
            }
        }

        repositorio.save(entidad);
        return DireccionResponse.de(entidad);
    }

    // Borra una direccion propia del usuario y, si era la favorita, pasa esa marca a la siguiente direccion disponible.
    @Override
    @Transactional
    public void eliminar(Integer idDireccion) {
        Usuario usuario = usuarioActual.requerir();
        // Comprueba que la direccion pertenezca a esta persona antes de borrarla.
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        boolean eraPredeterminada = Boolean.TRUE.equals(entidad.getPredeterminada());

        repositorio.delete(entidad);

        // Si se elimino la direccion favorita, la primera que quede pasa a ser la nueva favorita.
        if (eraPredeterminada) {
            repositorio.findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(usuario.getIdUsuario())
                    .stream().findFirst()
                    .ifPresent(siguiente -> {
                        siguiente.setPredeterminada(true);
                        repositorio.save(siguiente);
                    });
        }
    }

    // Cambia cual direccion queda como favorita para envios y desmarca cualquier otra que lo fuera antes.
    @Override
    @Transactional
    public DireccionResponse marcarPredeterminada(Integer idDireccion) {
        Usuario usuario = usuarioActual.requerir();
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        fijarPredeterminada(usuario.getIdUsuario(), idDireccion);
        entidad.setPredeterminada(true);
        return DireccionResponse.de(entidad);
    }

    // Entrega la direccion favorita del usuario (o la primera que tenga) para llenar automaticamente el envio al comprar.
    @Override
    @Transactional(readOnly = true)
    public DireccionResponse direccionPrincipal() {
        Usuario usuario = usuarioActual.requerir();
        return repositorio.findFirstByIdUsuarioAndPredeterminadaTrue(usuario.getIdUsuario())
                .or(() -> repositorio.findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(usuario.getIdUsuario())
                        .stream().findFirst())
                .map(DireccionResponse::de)
                .orElse(null);
    }

    /* ----------------------------- Internos -------------------------- */

    // Quita la marca de favorita a las demas direcciones del usuario para que solo una sea la principal.
    private void fijarPredeterminada(Integer idUsuario, Integer idElegida) {
        List<DireccionUsuario> todas = repositorio
                .findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(idUsuario);
        for (DireccionUsuario d : todas) {
            boolean debeSer = d.getIdDireccion().equals(idElegida);
            if (!debeSer && Boolean.TRUE.equals(d.getPredeterminada())) {
                d.setPredeterminada(false);
                d.setActualizadoEn(LocalDateTime.now());
                repositorio.save(d);
            }
        }
    }

    // Verifica que la direccion exista y realmente pertenezca a la cuenta conectada.
    private DireccionUsuario propiaDelUsuario(Integer idDireccion, Integer idUsuario) {
        if (idDireccion == null) {
            throw ApiException.badRequest("Direccion no indicada");
        }
        return repositorio.findByIdDireccionAndIdUsuario(idDireccion, idUsuario)
                .orElseThrow(() -> ApiException.notFound("Direccion no encontrada"));
    }

    // Copia y limpia los datos del formulario (direccion, ciudad, barrio, telefono, quien recibe y ubicacion en mapa).
    private void aplicar(DireccionUsuario entidad, DireccionRequest r) {
        entidad.setEtiqueta(r.etiqueta().trim());
        entidad.setDireccion(r.direccion().trim());
        entidad.setCiudad(limpiar(r.ciudad()));
        entidad.setBarrio(limpiar(r.barrio()));
        entidad.setTelefono(PatronesValidacion.normalizarTelefono(r.telefono()));
        entidad.setNombreRecibe(limpiar(r.nombreRecibe()));
        entidad.setReferencia(limpiar(r.referencia()));
        entidad.setLatitud(r.latitud());
        entidad.setLongitud(r.longitud());
        entidad.setActualizadoEn(LocalDateTime.now());
    }

    // Revisa que la direccion sea suficientemente clara (minimo 8 caracteres) y que el telefono tenga entre 7 y 10 numeros.
    private void validar(DireccionRequest r) {
        if (r.direccion() == null || r.direccion().trim().length() < 8) {
            throw ApiException.badRequest("La direccion debe tener al menos 8 caracteres");
        }
        if (StringUtils.hasText(r.telefono()) && !PatronesValidacion.telefonoValido(r.telefono())) {
            throw ApiException.badRequest("El telefono debe tener entre 7 y 10 digitos");
        }
    }

    // Quita espacios sobrantes y devuelve vacio si el campo no tenia texto util.
    private String limpiar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
