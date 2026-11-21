# ---------------------------------------------------------------------
# TECNISECURE v2 - Dockerfile raiz para despliegue en la nube (Render)
# Empaqueta tanto el backend Spring Boot (Java 21) como el frontend
# estatico en una sola imagen lista para correr con MySQL/TiDB o H2.
# ---------------------------------------------------------------------

# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 --gid 0 tecnicsecure \
    && chown tecnicsecure:0 /app && chmod 775 /app

COPY --from=build /build/target/tecnicsecure-api-*.jar /app/app.jar
COPY frontend /app/frontend

RUN mkdir -p /app/data /app/frontend/uploads /app/frontend/uploads/evidencias /app/frontend/uploads/productos \
    && chown -R 10001:0 /app

USER 10001

# Ajustado para correr holgado dentro de los 512 MB RAM del plan gratuito de Render.
ENV PORT=8080 \
    UPLOADS_DIR=/app/frontend/uploads \
    TZ=America/Bogota \
    JAVA_OPTS="-Dfrontend.dir=/app/frontend -Dspring.web.resources.static-locations=file:/app/frontend/ -Xms128m -Xmx320m -Dfile.encoding=UTF-8 -Duser.timezone=America/Bogota"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
