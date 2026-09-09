# Imagen de desarrollo: el código va montado desde el host, no copiado.
#
# Por eso aquí no hay COPY ni build. Lo único que aporta la imagen es el JDK 25
# que pide el toolchain de `build.gradle`; el wrapper, las fuentes y las
# dependencias llegan por el montaje y por los volúmenes de docker-compose.
#
# La contrapartida, y hay que saberla: **Java no tiene recarga en caliente**.
# Un cambio en el backend se aplica con `docker compose restart backend`, que
# recompila lo que cambió y vuelve a arrancar.
FROM eclipse-temurin:25-jdk

# Gradle guarda aquí su caché —dependencias y wrapper—, y docker-compose la
# monta en un volumen con nombre para que se descargue una sola vez.
#
# No vale `/root/.gradle`: el contenedor corre con el usuario del host para que
# los ficheros que escriba —las fotos de `uploads/`— no queden siendo de root en
# el repositorio.
ENV GRADLE_USER_HOME=/gradle

WORKDIR /app

# Los tres directorios que docker-compose monta como volumen, creados aquí a
# propósito: un volumen con nombre hereda el dueño de la ruta que tapa, y si la
# ruta no existe en la imagen nace siendo de root. El contenedor corre como
# 1000 y Gradle se quedaría sin poder escribir su caché.
RUN mkdir -p /gradle /app/build /app/.gradle \
    && chown -R 1000:1000 /gradle /app

EXPOSE 8080

# Sin demonio: el contenedor ya es el proceso de larga vida, y un demonio dentro
# solo deja un proceso huérfano al parar.
CMD ["./gradlew", "--no-daemon", "bootRun"]
