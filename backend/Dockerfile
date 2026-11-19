# ---------------------------------------------------------------------
# TECNISECURE - imagen del backend (Spring Boot + H2 en archivo)
# Compila con Maven dentro de la imagen: solo hace falta Docker.
# ---------------------------------------------------------------------

# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Se copian primero las dependencias para aprovechar la cache de Docker
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios (la imagen JRE ya trae el grupo "users")
RUN useradd --system --uid 10001 --gid 0 tecnicsecure \
    && chown tecnicsecure:0 /app && chmod 775 /app

COPY --from=build /build/target/tecnicsecure-api-*.jar /app/app.jar

# Carpetas persistentes: base de datos y archivos subidos.
# /app/frontend contiene el sitio web (se monta desde ./frontend).
RUN mkdir -p /app/data /app/frontend \
    && chown -R 10001:0 /app

USER 10001

# Rutas absolutas: no dependen del directorio de trabajo ni del sistema.
ENV APP_PORT=8080 \
    UPLOADS_DIR=/app/frontend/uploads \
    JAVA_OPTS="-Dfrontend.dir=/app/frontend -Dspring.web.resources.static-locations=file:/app/frontend/ -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Duser.timezone=America/Bogota"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=20 \
    CMD ["sh", "-c", "curl -fsS http://127.0.0.1:${APP_PORT}/actuator/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
