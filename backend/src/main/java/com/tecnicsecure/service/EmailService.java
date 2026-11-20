package com.tecnicsecure.service;

import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.repository.LogActividadRepository;
import com.tecnicsecure.util.UsuarioActual;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio centralizado de envio real de correos y documentos adjuntos.
 *
 * Cuando hay credenciales SMTP (MAIL_USERNAME + MAIL_PASSWORD) envia el correo
 * real con adjunto usando JavaMailSender. Si no hay credenciales, el documento
 * queda auditado y se informa al usuario en lenguaje sencillo (sin exponer
 * nombres de variables de entorno).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final LogActividadRepository logRepository;
    private final UsuarioActual usuarioActual;
    private JavaMailSender mailSender;

    @Value("${tecnisecure.mail.habilitado:false}")
    private boolean habilitado;

    @Value("${tecnisecure.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${tecnisecure.mail.remitente:no-responder@tecnisecure.com}")
    private String remitente;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public EmailService(LogActividadRepository logRepository, UsuarioActual usuarioActual) {
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Indica si el servidor esta listo para enviar correos reales. */
    public boolean estaConfigurado() {
        return habilitado && mailSender != null
                && StringUtils.hasText(smtpUsername) && StringUtils.hasText(smtpPassword);
    }

    /** Estado del servicio para mostrarlo en el panel (sin exponer credenciales). */
    public Map<String, Object> estado() {
        return Map.of(
                "configurado", estaConfigurado(),
                "servidor", host,
                "remitente", tieneTexto(smtpUsername) ? smtpUsername : remitente
        );
    }

    public Map<String, String> enviarCorreo(String destinatario, String asunto, String resumenContenido) {
        return enviarCorreoConAdjunto(destinatario, asunto,
                "<p>" + (resumenContenido != null ? resumenContenido : "") + "</p>",
                null, null, resumenContenido);
    }

    public Map<String, String> enviarCorreoConAdjunto(String destinatario,
                                                      String asunto,
                                                      String cuerpoHtml,
                                                      String nombreAdjunto,
                                                      byte[] bytesAdjunto,
                                                      String resumenAuditoria) {
        String destino = destinatario == null ? "" : destinatario.trim();
        String estado;
        String mensajeEstado;

        if (!StringUtils.hasText(destino) || !destino.contains("@")) {
            estado = "SIN_DESTINATARIO";
            mensajeEstado = "Este comprobante no tiene un correo válido registrado, no se pudo enviar.";
        } else if (!estaConfigurado()) {
            // Sin credenciales: el documento queda registrado pero no sale.
            estado = "CORREO_NO_CONFIGURADO";
            mensajeEstado = "El envío de correos no está configurado en este equipo. "
                    + "El comprobante quedó registrado y puedes descargarlo o imprimirlo.";
            log.info("[EMAIL:SIN_CONFIGURAR] Para={} | Asunto={}", destino, asunto);
        } else {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
                String from = StringUtils.hasText(remitente) ? remitente : smtpUsername;
                helper.setFrom(from, "TECNISECURE");
                helper.setTo(destino);
                helper.setSubject(asunto);
                helper.setText(cuerpoHtml != null ? cuerpoHtml : "", true);
                if (StringUtils.hasText(nombreAdjunto) && bytesAdjunto != null && bytesAdjunto.length > 0) {
                    helper.addAttachment(nombreAdjunto, new ByteArrayResource(bytesAdjunto), "text/html");
                }
                mailSender.send(mime);
                estado = "ENVIADO";
                mensajeEstado = "Correo enviado a " + destino + " con el comprobante adjunto.";
                log.info("[EMAIL:ENVIADO] Para={} | Asunto={} | Adjunto={}", destino, asunto, nombreAdjunto);
            } catch (Exception ex) {
                estado = "ERROR_ENVIO";
                mensajeEstado = "No se pudo entregar el correo a " + destino + ". "
                        + "Revisa la dirección o intenta más tarde.";
                log.error("[EMAIL:ERROR] Para={} | Error={}", destino, ex.getMessage());
            }
        }

        LogActividad registro = new LogActividad();
        registro.setIdUsuario(usuarioActual.idOpcional());
        registro.setAccion("envio_correo");
        registro.setDetalle("Para: " + destino + " | Asunto: " + asunto + " | Estado: " + estado
                + (StringUtils.hasText(resumenAuditoria) ? " | " + resumenAuditoria : ""));
        logRepository.save(registro);

        return Map.of(
                "estado", estado,
                "mensaje", mensajeEstado,
                "destinatario", destino,
                "asunto", asunto,
                "servidor", host
        );
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
