/*
 * Este archivo se encarga de enviar correos electronicos a clientes y usuarios (por ejemplo,
 * facturas de compra, comprobantes o enlaces para recuperar contrasenas). Si la cuenta de correo
 * de la tienda no esta configurada todavia, igual guarda constancia del intento en el registro
 * de actividad y avisa con un mensaje claro que el comprobante se puede descargar o imprimir.
 */
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

    // Prepara el registro de actividad y los datos de la persona conectada para auditar cada correo enviado.
    public EmailService(LogActividadRepository logRepository, UsuarioActual usuarioActual) {
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
    }

    // Conecta el cartero electronico de la tienda cuando el equipo tiene correo habilitado.
    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Revisa si la tienda ya tiene encendido el correo y configurados su usuario y contrasena de envio.
    public boolean estaConfigurado() {
        return habilitado && mailSender != null
                && StringUtils.hasText(smtpUsername) && StringUtils.hasText(smtpPassword);
    }

    // Informa al panel administrativo si el correo esta listo y que direccion remitente usa, sin mostrar contrasenas.
    public Map<String, Object> estado() {
        return Map.of(
                "configurado", estaConfigurado(),
                "servidor", host,
                "remitente", tieneTexto(smtpUsername) ? smtpUsername : remitente
        );
    }

    // Envia un correo sencillo de solo texto (sin archivos adjuntos) al destinatario indicado.
    public Map<String, String> enviarCorreo(String destinatario, String asunto, String resumenContenido) {
        return enviarCorreoConAdjunto(destinatario, asunto,
                "<p>" + (resumenContenido != null ? resumenContenido : "") + "</p>",
                null, null, resumenContenido);
    }

    // Envia un correo con diseno visual y, si aplica, con la factura o comprobante adjunto; siempre deja constancia en auditoria.
    public Map<String, String> enviarCorreoConAdjunto(String destinatario,
                                                      String asunto,
                                                      String cuerpoHtml,
                                                      String nombreAdjunto,
                                                      byte[] bytesAdjunto,
                                                      String resumenAuditoria) {
        String destino = destinatario == null ? "" : destinatario.trim();
        String estado;
        String mensajeEstado;

        // Primero revisa que la direccion del destinatario exista y parezca un correo valido.
        if (!StringUtils.hasText(destino) || !destino.contains("@")) {
            estado = "SIN_DESTINATARIO";
            mensajeEstado = "Este comprobante no tiene un correo válido registrado, no se pudo enviar.";
        } else if (!estaConfigurado()) {
            // Si el correo de la tienda aun no esta configurado, informa con claridad sin bloquear al usuario.
            estado = "CORREO_NO_CONFIGURADO";
            mensajeEstado = "El envío de correos no está configurado en este equipo. "
                    + "El comprobante quedó registrado y puedes descargarlo o imprimirlo.";
            log.info("[EMAIL:SIN_CONFIGURAR] Para={} | Asunto={}", destino, asunto);
        } else {
            // Arma el correo real con su remitente, asunto, contenido visual y archivo adjunto si corresponde.
            try {
                boolean conAdjunto = StringUtils.hasText(nombreAdjunto)
                        && bytesAdjunto != null && bytesAdjunto.length > 0;

                MimeMessage mime = mailSender.createMimeMessage();
                // Activa el soporte para archivos adjuntos unicamente cuando de verdad hay un documento que anexar.
                MimeMessageHelper helper = new MimeMessageHelper(mime, conAdjunto, StandardCharsets.UTF_8.name());
                String from = StringUtils.hasText(remitente) ? remitente : smtpUsername;
                helper.setFrom(from, "TECNISECURE");
                helper.setTo(destino);
                helper.setSubject(asunto);
                helper.setText(cuerpoHtml != null ? cuerpoHtml : "", true);
                if (conAdjunto) {
                    helper.addAttachment(nombreAdjunto, new ByteArrayResource(bytesAdjunto), "text/html");
                }
                mailSender.send(mime);
                estado = "ENVIADO";
                mensajeEstado = conAdjunto
                        ? "Correo enviado a " + destino + " con el comprobante adjunto."
                        : "Factura enviada a " + destino + " en el cuerpo del mensaje.";
                log.info("[EMAIL:ENVIADO] Para={} | Asunto={} | Adjunto={}", destino, asunto,
                        conAdjunto ? nombreAdjunto : "ninguno");
            } catch (Exception ex) {
                estado = "ERROR_ENVIO";
                mensajeEstado = "No se pudo entregar el correo a " + destino + ". "
                        + "Revisa la dirección o intenta más tarde.";
                log.error("[EMAIL:ERROR] Para={} | Error={}", destino, ex.getMessage());
            }
        }

        // Anota el resultado en el historial de auditoria para saber siempre si el correo salio o fallo.
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

    // Revisa rapidamente si un texto tiene contenido util y no esta vacio.
    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
