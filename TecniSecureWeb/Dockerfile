# Este archivo es la receta paso a paso para empaquetar toda la tienda (tanto el servidor
# como las pantallas visibles) dentro de una caja portátil lista para publicarse en la nube.
# Primero construye el programa y luego deja únicamente lo necesario para que funcione liviano y seguro.

# Primera parte: prepara las piezas y construye el programa de la tienda
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Descarga primero la lista de piezas externas para acelerar futuras construcciones
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

# Copia el código del servidor y genera el paquete ejecutable final
COPY backend/src ./src
RUN mvn -B -q clean package -DskipTests

# Segunda parte: crea la caja final liviana solo con lo necesario para encender la tienda
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crea una cuenta con permisos limitados por seguridad para que el servidor no corra como dueño total del equipo
RUN useradd --system --uid 10001 --gid 0 tecnicsecure \
    && chown tecnicsecure:0 /app && chmod 775 /app

# Copia dentro de la caja el servidor ya armado junto con todas las pantallas e imágenes de la web
COPY --from=build /build/target/tecnicsecure-api-*.jar /app/app.jar
COPY frontend /app/frontend

# Crea las carpetas donde se guardan la base de datos y las fotos o documentos subidos por los usuarios
RUN mkdir -p /app/data /app/frontend/uploads /app/frontend/uploads/evidencias /app/frontend/uploads/productos \
    && chown -R 10001:0 /app

# Cambia a la cuenta segura de permisos limitados antes de arrancar la tienda
USER 10001

# Ajusta el puerto, la hora de Colombia, el soporte de tildes y un uso moderado de memoria para servidores gratuitos
ENV PORT=8080 \
    UPLOADS_DIR=/app/frontend/uploads \
    TZ=America/Bogota \
    JAVA_OPTS="-Dfrontend.dir=/app/frontend -Dspring.web.resources.static-locations=file:/app/frontend/ -Xms128m -Xmx320m -Dfile.encoding=UTF-8 -Duser.timezone=America/Bogota"

# Indica que la tienda atenderá visitas por la puerta web 8080
EXPOSE 8080

# Orden final que enciende la tienda apenas se abre esta caja portátil
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
