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

    /** Tope para evitar que un usuario acumule direcciones sin limite. */
    private static final int MAXIMO_POR_USUARIO = 15;

    private final DireccionUsuarioRepository repositorio;
    private final UsuarioActual usuarioActual;

    public DireccionServiceImpl(DireccionUsuarioRepository repositorio, UsuarioActual usuarioActual) {
        this.repositorio = repositorio;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponse> misDirecciones() {
        Usuario usuario = usuarioActual.requerir();
        return repositorio.findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(usuario.getIdUsuario())
                .stream().map(DireccionResponse::de).toList();
    }

    @Override
    @Transactional
    public DireccionResponse crear(DireccionRequest request) {
        Usuario usuario = usuarioActual.requerir();
        validar(request);

        if (repositorio.countByIdUsuario(usuario.getIdUsuario()) >= MAXIMO_POR_USUARIO) {
            throw ApiException.badRequest("Ya tienes " + MAXIMO_POR_USUARIO
                    + " direcciones guardadas. Elimina una para agregar otra.");
        }

        DireccionUsuario entidad = new DireccionUsuario();
        entidad.setIdUsuario(usuario.getIdUsuario());
        aplicar(entidad, request);

        // La primera direccion siempre queda como predeterminada.
        boolean primera = repositorio.countByIdUsuario(usuario.getIdUsuario()) == 0;
        boolean marcar = Boolean.TRUE.equals(request.predeterminada()) || primera;

        repositorio.save(entidad);
        if (marcar) {
            fijarPredeterminada(usuario.getIdUsuario(), entidad.getIdDireccion());
            entidad.setPredeterminada(true);
        }
        return DireccionResponse.de(entidad);
    }

    @Override
    @Transactional
    public DireccionResponse actualizar(Integer idDireccion, DireccionRequest request) {
        Usuario usuario = usuarioActual.requerir();
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        validar(request);

        aplicar(entidad, request);
        entidad.setActualizadoEn(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.predeterminada())) {
            fijarPredeterminada(usuario.getIdUsuario(), idDireccion);
            entidad.setPredeterminada(true);
        } else if (Boolean.FALSE.equals(request.predeterminada())
                && Boolean.TRUE.equals(entidad.getPredeterminada())) {
            // No se permite dejar el conjunto sin predeterminada si hay mas de una.
            long total = repositorio.countByIdUsuario(usuario.getIdUsuario());
            if (total == 1) {
                entidad.setPredeterminada(true);
            }
        }

        repositorio.save(entidad);
        return DireccionResponse.de(entidad);
    }

    @Override
    @Transactional
    public void eliminar(Integer idDireccion) {
        Usuario usuario = usuarioActual.requerir();
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        boolean eraPredeterminada = Boolean.TRUE.equals(entidad.getPredeterminada());

        repositorio.delete(entidad);

        // Si se borro la predeterminada, la primera que quede toma el relevo.
        if (eraPredeterminada) {
            repositorio.findByIdUsuarioOrderByPredeterminadaDescIdDireccionAsc(usuario.getIdUsuario())
                    .stream().findFirst()
                    .ifPresent(siguiente -> {
                        siguiente.setPredeterminada(true);
                        repositorio.save(siguiente);
                    });
        }
    }

    @Override
    @Transactional
    public DireccionResponse marcarPredeterminada(Integer idDireccion) {
        Usuario usuario = usuarioActual.requerir();
        DireccionUsuario entidad = propiaDelUsuario(idDireccion, usuario.getIdUsuario());
        fijarPredeterminada(usuario.getIdUsuario(), idDireccion);
        entidad.setPredeterminada(true);
        return DireccionResponse.de(entidad);
    }

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

    private DireccionUsuario propiaDelUsuario(Integer idDireccion, Integer idUsuario) {
        if (idDireccion == null) {
            throw ApiException.badRequest("Direccion no indicada");
        }
        return repositorio.findByIdDireccionAndIdUsuario(idDireccion, idUsuario)
                .orElseThrow(() -> ApiException.notFound("Direccion no encontrada"));
    }

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

    private void validar(DireccionRequest r) {
        if (r.direccion() == null || r.direccion().trim().length() < 8) {
            throw ApiException.badRequest("La direccion debe tener al menos 8 caracteres");
        }
        if (StringUtils.hasText(r.telefono()) && !PatronesValidacion.telefonoValido(r.telefono())) {
            throw ApiException.badRequest("El telefono debe tener entre 7 y 10 digitos");
        }
    }

    private String limpiar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
