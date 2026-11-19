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
 * Cuando SMTP esta configurado (MAIL_ENABLED=true y credenciales spring.mail.username/password),
 * envia el correo real con adjunto usando JavaMailSender; de lo contrario informa claramente
 * el estado local y deja rastro auditado sin exponer datos de terceros.
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

    public EmailService(LogActividadRepository logRepository, UsuarioActual usuarioActual) {
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
        boolean smtpListo = habilitado && mailSender != null && StringUtils.hasText(smtpUsername);
        String estado = "SIMULADO_SIN_SMTP";
        String mensajeEstado = "Documento preparado localmente para " + destinatario
                + " (activa MAIL_ENABLED=true, MAIL_USERNAME y MAIL_PASSWORD para despacho SMTP real).";

        if (smtpListo) {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
                String from = StringUtils.hasText(remitente) ? remitente : smtpUsername;
                helper.setFrom(from);
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(cuerpoHtml != null ? cuerpoHtml : "", true);
                if (StringUtils.hasText(nombreAdjunto) && bytesAdjunto != null && bytesAdjunto.length > 0) {
                    helper.addAttachment(nombreAdjunto, new ByteArrayResource(bytesAdjunto));
                }
                mailSender.send(mime);
                estado = "ENVIADO_SMTP";
                mensajeEstado = "Correo y documento adjunto enviados realmente a " + destinatario + ".";
                log.info("[EMAIL:SMTP_REAL] Para={} | Asunto={} | Adjunto={}", destinatario, asunto, nombreAdjunto);
            } catch (Exception ex) {
                estado = "ERROR_SMTP";
                mensajeEstado = "No se pudo enviar por SMTP a " + destinatario + ": " + ex.getMessage();
                log.error("[EMAIL:ERROR_SMTP] Para={} | Error={}", destinatario, ex.getMessage());
            }
        } else {
            log.info("[EMAIL:LOCAL_AUDITADO] Para={} | Asunto={} | Remitente={}", destinatario, asunto, remitente);
        }

        LogActividad registro = new LogActividad();
        registro.setIdUsuario(usuarioActual.idOpcional());
        registro.setAccion("envio_correo");
        registro.setDetalle("Para: " + destinatario + " | Asunto: " + asunto + " | Estado: " + estado
                + (StringUtils.hasText(resumenAuditoria) ? " | " + resumenAuditoria : ""));
        logRepository.save(registro);

        return Map.of(
                "estado", estado,
                "mensaje", mensajeEstado,
                "destinatario", destinatario,
                "asunto", asunto,
                "servidor", host
        );
    }
}
