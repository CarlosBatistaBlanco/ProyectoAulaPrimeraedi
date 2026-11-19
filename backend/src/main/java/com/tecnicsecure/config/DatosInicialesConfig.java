package com.tecnicsecure.config;

import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.repository.RolRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Garantiza que exista la cuenta administrador al arrancar, completa fechas faltantes
 * y repara textos guardados con doble codificacion UTF-8 (mojibake).
 * La contrasena se puede cambiar con ADMIN_PASSWORD en las variables de entorno.
 */
@Configuration
public class DatosInicialesConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesConfig.class);

    private static final Map<Character, Byte> CP1252_EXTRA = Map.ofEntries(
            Map.entry('\u20AC', (byte) 0x80), Map.entry('\u201A', (byte) 0x82), Map.entry('\u0192', (byte) 0x83),
            Map.entry('\u201E', (byte) 0x84), Map.entry('\u2026', (byte) 0x85), Map.entry('\u2020', (byte) 0x86),
            Map.entry('\u2021', (byte) 0x87), Map.entry('\u02C6', (byte) 0x88), Map.entry('\u2030', (byte) 0x89),
            Map.entry('\u0160', (byte) 0x8A), Map.entry('\u2039', (byte) 0x8B), Map.entry('\u0152', (byte) 0x8C),
            Map.entry('\u017D', (byte) 0x8E), Map.entry('\u2018', (byte) 0x91), Map.entry('\u2019', (byte) 0x92),
            Map.entry('\u201C', (byte) 0x93), Map.entry('\u201D', (byte) 0x94), Map.entry('\u2022', (byte) 0x95),
            Map.entry('\u2013', (byte) 0x96), Map.entry('\u2014', (byte) 0x97), Map.entry('\u02DC', (byte) 0x98),
            Map.entry('\u2122', (byte) 0x99), Map.entry('\u0161', (byte) 0x9A), Map.entry('\u203A', (byte) 0x9B),
            Map.entry('\u0153', (byte) 0x9C), Map.entry('\u017E', (byte) 0x9E), Map.entry('\u0178', (byte) 0x9F)
    );

    @Bean
    public ApplicationRunner crearAdministrador(UsuarioRepository usuarioRepository,
                                                RolRepository rolRepository,
                                                PasswordEncoder passwordEncoder,
                                                TecniSecureProperties propiedades,
                                                JdbcTemplate jdbcTemplate,
                                                DataSource dataSource) {
        return args -> {
            completarFechasFaltantes(jdbcTemplate);
            repararCodificacionMojibake(dataSource);

            String correo = propiedades.admin().correo();

            if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
                log.info("Usuario administrador ya existe: {}", correo);
                return;
            }

            Rol rolAdmin = rolRepository.findByNombre(Rol.ADMINISTRADOR).orElseGet(() -> {
                Rol nuevo = new Rol();
                nuevo.setNombre(Rol.ADMINISTRADOR);
                return rolRepository.save(nuevo);
            });

            Usuario admin = new Usuario();
            admin.setRol(rolAdmin);
            admin.setNombre("Administrador Principal");
            admin.setCorreo(correo.toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(propiedades.admin().password()));
            admin.setActivo(true);
            admin.setBloqueado(false);
            admin.setIntentosFallidos(0);
            usuarioRepository.save(admin);

            log.info("Usuario administrador creado: {}", correo);
        };
    }

    private void completarFechasFaltantes(JdbcTemplate jdbcTemplate) {
        String[] tablasConFecha = {
                "usuarios", "clientes", "productos", "pedidos", "pedido_historial",
                "facturas", "notificaciones", "mensajes_chat", "logs_actividad",
                "inventario_movimientos", "solicitudes_servicio", "contactos", "resenas"
        };
        for (String tabla : tablasConFecha) {
            try {
                jdbcTemplate.update("UPDATE " + tabla + " SET creado_en = CURRENT_TIMESTAMP WHERE creado_en IS NULL");
            } catch (Exception ignored) {
                // Tabla aun no poblada o sin columna creado_en
            }
        }
    }

    private void repararCodificacionMojibake(DataSource dataSource) {
        int totalCorregidos = 0;
        try (Connection con = dataSource.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            String esquema = con.getSchema();
            List<String> tablas = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, esquema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tablas.add(rs.getString("TABLE_NAME"));
                }
            }
            for (String tabla : tablas) {
                List<String> pks = new ArrayList<>();
                try (ResultSet rs = meta.getPrimaryKeys(null, esquema, tabla)) {
                    while (rs.next()) {
                        pks.add(rs.getString("COLUMN_NAME"));
                    }
                }
                if (pks.size() != 1) {
                    continue;
                }
                String pkCol = pks.get(0);
                List<String> colsTexto = new ArrayList<>();
                try (ResultSet rs = meta.getColumns(null, esquema, tabla, null)) {
                    while (rs.next()) {
                        int sqlType = rs.getInt("DATA_TYPE");
                        if (sqlType == Types.VARCHAR || sqlType == Types.NVARCHAR
                                || sqlType == Types.LONGVARCHAR || sqlType == Types.CLOB) {
                            colsTexto.add(rs.getString("COLUMN_NAME"));
                        }
                    }
                }
                for (String col : colsTexto) {
                    totalCorregidos += corregirColumna(con, tabla, pkCol, col);
                }
            }
            if (totalCorregidos > 0) {
                log.info("Saneamiento de caracteres UTF-8 aplicado sobre {} celda(s)", totalCorregidos);
            }
        } catch (Exception ex) {
            log.warn("No se pudo completar el saneamiento de codificacion: {}", ex.getMessage());
        }
    }

    private int corregirColumna(Connection con, String tabla, String pkCol, String col) {
        int corregidos = 0;
        String selectSql = "SELECT \"" + pkCol + "\", \"" + col + "\" FROM \"" + tabla + "\" "
                + "WHERE \"" + col + "\" LIKE '%\u00C3%' OR \"" + col + "\" LIKE '%\u00C2%'";
        String updateSql = "UPDATE \"" + tabla + "\" SET \"" + col + "\" = ? WHERE \"" + pkCol + "\" = ?";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(selectSql);
             PreparedStatement ps = con.prepareStatement(updateSql)) {
            while (rs.next()) {
                Object pk = rs.getObject(1);
                String original = rs.getString(2);
                String limpio = decodificarMojibakeUtf8(original);
                if (limpio != null && !limpio.equals(original)) {
                    ps.setString(1, limpio);
                    ps.setObject(2, pk);
                    ps.executeUpdate();
                    corregidos++;
                }
            }
        } catch (Exception ignored) {
            // Columna o tabla no compatible
        }
        return corregidos;
    }

    static String decodificarMojibakeUtf8(String valor) {
        if (valor == null || (valor.indexOf('\u00C3') < 0 && valor.indexOf('\u00C2') < 0)) {
            return valor;
        }
        String actual = valor;
        for (int intento = 0; intento < 2; intento++) {
            byte[] bytes = recuperarBytesCp1252(actual);
            if (bytes == null) {
                break;
            }
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                CharBuffer cb = decoder.decode(ByteBuffer.wrap(bytes));
                String candidato = cb.toString();
                if (!candidato.equals(actual) && candidato.length() < actual.length()) {
                    actual = candidato;
                } else {
                    break;
                }
            } catch (CharacterCodingException e) {
                break;
            }
        }
        return actual;
    }

    private static byte[] recuperarBytesCp1252(String s) {
        byte[] out = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0xFF) {
                out[i] = (byte) c;
            } else {
                Byte b = CP1252_EXTRA.get(c);
                if (b == null) {
                    return null;
                }
                out[i] = b;
            }
        }
        return out;
    }
}
